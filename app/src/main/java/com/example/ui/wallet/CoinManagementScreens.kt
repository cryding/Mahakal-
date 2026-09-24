package com.example.ui.wallet

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
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
import androidx.compose.ui.window.Dialog
import com.example.backend.model.AccountCoinSummaryDto
import com.example.backend.model.AgentSummaryDto
import com.example.backend.model.TransactionDto
import com.example.backend.model.UserSummaryDto

// Design System Palette
private val BgDark = Color(0xFF0A0E17)
private val CardBg = Color(0xFF141C28)
private val CardBorder = Color(0xFF1E2B3E)
private val AccentGold = Color(0xFFE5A93C)
private val AgentBlue = Color(0xFF42A5F5)
private val SuccessGreen = Color(0xFF4CAF50)
private val DangerRed = Color(0xFFEF5350)
private val WarningAmber = Color(0xFFFFB74D)
private val TextMuted = Color(0xFF8A99AD)

/**
 * Reusable Transfer Coin Screen / Modal.
 * Supports Admin -> Agent and Agent -> User transfer operations.
 */
@Composable
fun TransferCoinScreen(
    role: String,
    senderName: String,
    availableBalance: Long,
    recipientAccountId: String,
    recipientDisplayName: String,
    isSubmitting: Boolean = false,
    onBack: () -> Unit,
    onSubmitTransfer: (amount: Long, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor = if (role == "ADMIN") AccentGold else AgentBlue
    var amountText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val amountLong = amountText.toLongOrNull() ?: 0L
    val isAmountValid = amountLong > 0L && amountLong <= availableBalance
    val isReasonValid = reasonText.trim().length >= 3
    val canSubmit = isAmountValid && isReasonValid && !isSubmitting

    val remainingPreview = (availableBalance - amountLong).coerceAtLeast(0L)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("transfer_coin_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("transfer_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (role == "ADMIN") "TRANSFER COINS TO AGENT" else "TRANSFER COINS TO USER",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Source: $senderName (Authoritative Server Wallet)",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Balance Overview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, themeColor.copy(alpha = 0.3f))))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "AVAILABLE BALANCE",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = themeColor,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "${formatCoins(availableBalance)} VIRTUAL COINS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Coins are non-monetary virtual units. Balance is strictly managed by server.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recipient Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, CardBorder)))
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = CircleShape,
                    color = themeColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Recipient",
                            tint = themeColor,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Recipient",
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    Text(
                        text = recipientDisplayName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        text = "ID: $recipientAccountId",
                        fontSize = 11.sp,
                        color = TextMuted,
                        fontFamily = FontFamily.Monospace
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Fields
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Transfer Amount (Coins)") },
            placeholder = { Text("e.g. 5000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = amountText.isNotEmpty() && !isAmountValid,
            supportingText = {
                if (amountText.isNotEmpty() && amountLong <= 0L) {
                    Text("Amount must be greater than 0", color = DangerRed)
                } else if (amountLong > availableBalance) {
                    Text("Amount exceeds available balance (${formatCoins(availableBalance)})", color = DangerRed)
                } else {
                    Text("Coins to allocate to recipient", color = TextMuted)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColor,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transfer_amount_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = reasonText,
            onValueChange = { reasonText = it },
            label = { Text("Transfer Reason / Audit Note") },
            placeholder = { Text("e.g. Weekly operational quota allocation") },
            isError = reasonText.isNotEmpty() && !isReasonValid,
            supportingText = {
                if (reasonText.isNotEmpty() && !isReasonValid) {
                    Text("Reason must be at least 3 characters", color = DangerRed)
                } else {
                    Text("Reason will be permanently recorded in the immutable ledger", color = TextMuted)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColor,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transfer_reason_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Balance Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1724)),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, CardBorder)))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Available Balance:", color = TextMuted, fontSize = 13.sp)
                    Text("${formatCoins(availableBalance)} coins", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Transfer Amount:", color = themeColor, fontSize = 13.sp)
                    Text("-${formatCoins(amountLong)} coins", color = themeColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = CardBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Remaining Balance (Preview):", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${formatCoins(remainingPreview)} coins", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: The displayed remaining balance is only a preview. The server recalculates authoritative balances atomically.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit Button
        Button(
            onClick = { showConfirmDialog = true },
            enabled = canSubmit,
            colors = ButtonDefaults.buttonColors(
                containerColor = themeColor,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("review_transfer_button")
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.Black, strokeWidth = 2.dp)
            } else {
                Icon(imageVector = Icons.Default.Send, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("REVIEW & CONFIRM TRANSFER", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        CoinConfirmationDialog(
            actionType = "TRANSFER",
            sourceAccount = senderName,
            targetAccount = "$recipientDisplayName ($recipientAccountId)",
            amount = amountLong,
            currentBalance = availableBalance,
            expectedRemainingBalance = remainingPreview,
            reason = reasonText,
            isSubmitting = isSubmitting,
            themeColor = themeColor,
            onDismiss = { showConfirmDialog = false },
            onConfirm = {
                showConfirmDialog = false
                onSubmitTransfer(amountLong, reasonText)
            }
        )
    }
}

/**
 * Reusable Deduct Coin Screen / Modal.
 * Supports Admin deduction from Agent and Agent deduction from User.
 */
@Composable
fun DeductCoinScreen(
    role: String,
    operatorName: String,
    targetAccountId: String,
    targetDisplayName: String,
    targetCurrentBalance: Long,
    isSubmitting: Boolean = false,
    onBack: () -> Unit,
    onSubmitDeduct: (amount: Long, reason: String) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColor = DangerRed
    var amountText by remember { mutableStateOf("") }
    var reasonText by remember { mutableStateOf("") }
    var showConfirmDialog by remember { mutableStateOf(false) }

    val amountLong = amountText.toLongOrNull() ?: 0L
    val isAmountValid = amountLong > 0L && amountLong <= targetCurrentBalance
    val isReasonValid = reasonText.trim().length >= 3
    val canSubmit = isAmountValid && isReasonValid && !isSubmitting

    val remainingPreview = (targetCurrentBalance - amountLong).coerceAtLeast(0L)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
            .testTag("deduct_coin_screen")
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("deduct_back_btn")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Column {
                Text(
                    text = if (role == "ADMIN") "DEDUCT COINS FROM AGENT" else "DEDUCT COINS FROM USER",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Operator: $operatorName (Authorized Scope)",
                    fontSize = 12.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Target Info Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, themeColor.copy(alpha = 0.4f))))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "TARGET ACCOUNT",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = targetDisplayName,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "ID: $targetAccountId",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )
                Spacer(modifier = Modifier.height(10.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = "Coins",
                        tint = AccentGold,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Current Balance: ${formatCoins(targetCurrentBalance)} coins",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Input Fields
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it.filter { ch -> ch.isDigit() } },
            label = { Text("Deduction Amount (Coins)") },
            placeholder = { Text("e.g. 1000") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            isError = amountText.isNotEmpty() && !isAmountValid,
            supportingText = {
                if (amountText.isNotEmpty() && amountLong <= 0L) {
                    Text("Amount must be greater than 0", color = DangerRed)
                } else if (amountLong > targetCurrentBalance) {
                    Text("Amount exceeds target's current balance (${formatCoins(targetCurrentBalance)})", color = DangerRed)
                } else {
                    Text("Coins to deduct and remove from target's wallet", color = TextMuted)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColor,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("deduct_amount_input")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = reasonText,
            onValueChange = { reasonText = it },
            label = { Text("Deduction Reason (Mandatory)") },
            placeholder = { Text("e.g. Administrative quota reclamation") },
            isError = reasonText.isNotEmpty() && !isReasonValid,
            supportingText = {
                if (reasonText.isNotEmpty() && !isReasonValid) {
                    Text("Reason must be at least 3 characters", color = DangerRed)
                } else {
                    Text("Audit trail justification required for all deductions", color = TextMuted)
                }
            },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColor,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg
            ),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("deduct_reason_input")
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Balance Preview Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF0F1724)),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, CardBorder)))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Target Current Balance:", color = TextMuted, fontSize = 13.sp)
                    Text("${formatCoins(targetCurrentBalance)} coins", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Deduction Amount:", color = themeColor, fontSize = 13.sp)
                    Text("-${formatCoins(amountLong)} coins", color = themeColor, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                }
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp), color = CardBorder)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Remaining Balance (Preview):", color = TextMuted, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                    Text("${formatCoins(remainingPreview)} coins", color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Note: Preview only. Backend enforces optimistic lock and balance integrity.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Submit Button
        Button(
            onClick = { showConfirmDialog = true },
            enabled = canSubmit,
            colors = ButtonDefaults.buttonColors(
                containerColor = themeColor,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("review_deduct_button")
        ) {
            if (isSubmitting) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
                Icon(imageVector = Icons.Default.RemoveCircleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("REVIEW & CONFIRM DEDUCTION", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }

    // Confirmation Dialog
    if (showConfirmDialog) {
        CoinConfirmationDialog(
            actionType = "DEDUCTION",
            sourceAccount = operatorName,
            targetAccount = "$targetDisplayName ($targetAccountId)",
            amount = amountLong,
            currentBalance = targetCurrentBalance,
            expectedRemainingBalance = remainingPreview,
            reason = reasonText,
            isSubmitting = isSubmitting,
            themeColor = themeColor,
            onDismiss = { showConfirmDialog = false },
            onConfirm = {
                showConfirmDialog = false
                onSubmitDeduct(amountLong, reasonText)
            }
        )
    }
}

/**
 * Confirmation Dialog for balance mutations.
 * Enforces double-tap protection, displays full breakdown, and guarantees review.
 */
@Composable
fun CoinConfirmationDialog(
    actionType: String, // "TRANSFER" or "DEDUCTION"
    sourceAccount: String,
    targetAccount: String,
    amount: Long,
    currentBalance: Long,
    expectedRemainingBalance: Long,
    reason: String,
    isSubmitting: Boolean,
    themeColor: Color = AccentGold,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = if (actionType == "TRANSFER") Icons.Default.Send else Icons.Default.Warning,
                    contentDescription = null,
                    tint = themeColor,
                    modifier = Modifier.size(22.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Confirm Coin $actionType",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = Color.White
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Please review the transaction parameters before confirming. This operation is recorded in the immutable audit ledger.",
                    fontSize = 12.sp,
                    color = TextMuted
                )
                Spacer(modifier = Modifier.height(14.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F1622),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        DetailRow("Action", actionType, themeColor)
                        DetailRow("Source", sourceAccount, Color.White)
                        DetailRow("Target / Dest", targetAccount, Color.White)
                        DetailRow("Amount", "${formatCoins(amount)} coins", themeColor, isBold = true)
                        DetailRow("Current Balance", "${formatCoins(currentBalance)} coins", Color.White)
                        DetailRow("Expected Balance", "${formatCoins(expectedRemainingBalance)} coins", Color.White)
                        DetailRow("Reason", reason, TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                Text(
                    text = "Virtual coins have NO monetary or cash value. No real currency is involved.",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(
                    containerColor = themeColor,
                    contentColor = if (themeColor == AccentGold) Color.Black else Color.White
                ),
                modifier = Modifier.testTag("confirm_coin_action_btn")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.White, strokeWidth = 2.dp)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Processing...")
                } else {
                    Text("Confirm")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting,
                modifier = Modifier.testTag("cancel_coin_action_btn")
            ) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = CardBg,
        tonalElevation = 6.dp
    )
}

/**
 * Success Screen / Dialog after a balance mutation.
 * Displays authoritative transaction ID, balances before and after, timestamp, and actions.
 */
@Composable
fun CoinTransactionSuccessDialog(
    transaction: TransactionDto,
    onViewTransaction: () -> Unit,
    onDone: () -> Unit
) {
    Dialog(onDismissRequest = onDone) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("coin_success_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(SuccessGreen.copy(alpha = 0.4f), CardBorder)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = SuccessGreen.copy(alpha = 0.15f),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Success",
                            tint = SuccessGreen,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Transaction Successful",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Authoritative ledger update completed",
                    fontSize = 12.sp,
                    color = TextMuted
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF0F1622),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        DetailRow("Transaction ID", transaction.transactionId, SuccessGreen, isMono = true)
                        DetailRow("Type", transaction.transactionType, Color.White)
                        DetailRow("Amount", "${formatCoins(transaction.amount)} coins", SuccessGreen, isBold = true)
                        if (transaction.balanceBeforeSource != null && transaction.balanceAfterSource != null) {
                            DetailRow("Source Prev", "${formatCoins(transaction.balanceBeforeSource)} coins", TextMuted)
                            DetailRow("Source New", "${formatCoins(transaction.balanceAfterSource)} coins", Color.White, isBold = true)
                        }
                        if (transaction.balanceBeforeDestination != null && transaction.balanceAfterDestination != null) {
                            DetailRow("Dest Prev", "${formatCoins(transaction.balanceBeforeDestination)} coins", TextMuted)
                            DetailRow("Dest New", "${formatCoins(transaction.balanceAfterDestination)} coins", Color.White, isBold = true)
                        }
                        DetailRow("Timestamp", formatLedgerDate(transaction.timestamp), TextMuted)
                        DetailRow("Reason", transaction.reason, TextMuted)
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onViewTransaction,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("view_tx_success_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Details")
                    }
                    Button(
                        onClick = onDone,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("done_tx_success_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = SuccessGreen, contentColor = Color.White)
                    ) {
                        Text("Done")
                    }
                }
            }
        }
    }
}

/**
 * Failure Screen / Dialog for transaction errors.
 * Clearly presents the specific business failure reason without exposing internal database errors.
 */
@Composable
fun CoinTransactionFailureDialog(
    errorCode: String,
    friendlyMessage: String,
    onRetry: () -> Unit,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("coin_failure_dialog"),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(DangerRed.copy(alpha = 0.5f), CardBorder)))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = CircleShape,
                    color = DangerRed.copy(alpha = 0.15f),
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.ErrorOutline,
                            contentDescription = "Error",
                            tint = DangerRed,
                            modifier = Modifier.size(34.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Transaction Failed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = "Error Code: $errorCode",
                    fontSize = 11.sp,
                    color = TextMuted,
                    fontFamily = FontFamily.Monospace
                )

                Spacer(modifier = Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF261318),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = friendlyMessage,
                        fontSize = 13.sp,
                        color = Color(0xFFFFB4AB),
                        modifier = Modifier.padding(14.dp),
                        textAlign = TextAlign.Center
                    )
                }

                Spacer(modifier = Modifier.height(20.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onClose,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("close_failure_btn"),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White)
                    ) {
                        Text("Close")
                    }
                    Button(
                        onClick = onRetry,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("retry_failure_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black)
                    ) {
                        Text("Retry")
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Agent Coin Management Screen (Admin View).
 * Displays Agent summary, current balance, transfer button, deduct button, and recent ledger transactions.
 */
@Composable
fun AgentCoinManagementScreen(
    agentSummary: AccountCoinSummaryDto,
    isLoading: Boolean,
    onBack: () -> Unit,
    onTransferClick: () -> Unit,
    onDeductClick: () -> Unit,
    onRefresh: () -> Unit,
    onSelectTransaction: (TransactionDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .testTag("agent_coin_management_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_agent_coins")) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "AGENT COIN LEDGER",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${agentSummary.fullName} (${agentSummary.loginId})",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_agent_coins_btn")) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentGold)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Balance & Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, AccentGold.copy(alpha = 0.4f))))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CURRENT COIN BALANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${formatCoins(agentSummary.balance)} COINS",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AccentGold
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (agentSummary.status == "ACTIVE") SuccessGreen.copy(alpha = 0.2f) else DangerRed.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = agentSummary.status,
                            color = if (agentSummary.status == "ACTIVE") SuccessGreen else DangerRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onTransferClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("agent_screen_transfer_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Transfer Coins", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDeductClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("agent_screen_deduct_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Deduct Coins", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Transactions Header
        Text(
            text = "RECENT LEDGER TRANSACTIONS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else if (agentSummary.recentTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(CardBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No recent coin transactions for this agent", color = TextMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(agentSummary.recentTransactions) { tx ->
                    CompactTransactionCard(
                        transaction = tx,
                        onClick = { onSelectTransaction(tx) }
                    )
                }
            }
        }
    }
}

/**
 * Dedicated User Coin Management Screen (Agent View).
 * Displays User summary, current balance, transfer button, deduct button, and recent transactions.
 */
@Composable
fun UserCoinManagementScreen(
    userSummary: AccountCoinSummaryDto,
    isLoading: Boolean,
    onBack: () -> Unit,
    onTransferClick: () -> Unit,
    onDeductClick: () -> Unit,
    onRefresh: () -> Unit,
    onSelectTransaction: (TransactionDto) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .testTag("user_coin_management_screen")
    ) {
        // Top Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = onBack, modifier = Modifier.testTag("back_from_user_coins")) {
                    Icon(imageVector = Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = Color.White)
                }
                Spacer(modifier = Modifier.width(6.dp))
                Column {
                    Text(
                        text = "USER COIN LEDGER",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        letterSpacing = 1.sp
                    )
                    Text(
                        text = "${userSummary.fullName} (${userSummary.loginId})",
                        fontSize = 12.sp,
                        color = TextMuted
                    )
                }
            }

            IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_user_coins_btn")) {
                Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = AgentBlue)
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Balance & Actions Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, AgentBlue.copy(alpha = 0.4f))))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("CURRENT COIN BALANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${formatCoins(userSummary.balance)} COINS",
                            fontSize = 22.sp,
                            fontWeight = FontWeight.Bold,
                            color = AgentBlue
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (userSummary.status == "ACTIVE") SuccessGreen.copy(alpha = 0.2f) else DangerRed.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = userSummary.status,
                            color = if (userSummary.status == "ACTIVE") SuccessGreen else DangerRed,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = onTransferClick,
                        colors = ButtonDefaults.buttonColors(containerColor = AgentBlue, contentColor = Color.White),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("user_screen_transfer_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowUpward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Transfer Coins", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }

                    OutlinedButton(
                        onClick = onDeductClick,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                        shape = RoundedCornerShape(8.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("user_screen_deduct_btn")
                    ) {
                        Icon(imageVector = Icons.Default.ArrowDownward, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Deduct Coins", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Recent Transactions Header
        Text(
            text = "RECENT LEDGER TRANSACTIONS",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxWidth().height(140.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AgentBlue)
            }
        } else if (userSummary.recentTransactions.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(CardBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No recent coin transactions for this user", color = TextMuted, fontSize = 13.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(userSummary.recentTransactions) { tx ->
                    CompactTransactionCard(
                        transaction = tx,
                        onClick = { onSelectTransaction(tx) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CompactTransactionCard(
    transaction: TransactionDto,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, CardBorder)))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = transaction.transactionType,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = formatLedgerDate(transaction.timestamp),
                    fontSize = 11.sp,
                    color = TextMuted
                )
                Text(
                    text = transaction.reason,
                    fontSize = 11.sp,
                    color = TextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${formatCoins(transaction.amount)} coins",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGold
                )
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = SuccessGreen.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = transaction.status,
                        fontSize = 10.sp,
                        color = SuccessGreen,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun DetailRow(
    label: String,
    value: String,
    valueColor: Color,
    isBold: Boolean = false,
    isMono: Boolean = false
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = TextMuted, fontSize = 12.sp)
        Text(
            text = value,
            color = valueColor,
            fontSize = 12.sp,
            fontWeight = if (isBold) FontWeight.Bold else FontWeight.Normal,
            fontFamily = if (isMono) FontFamily.Monospace else FontFamily.Default,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Admin Navigation: Coins -> Agent Management.
 * Lists all agents with Agent ID, Agent Name, Status, Current Coin Balance, Last Transaction, and Actions.
 */
@Composable
fun AdminCoinsDirectoryView(
    adminBalance: Long,
    agents: List<AgentSummaryDto>,
    accountBalances: Map<String, Long>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onTransferCoins: (AgentSummaryDto) -> Unit,
    onDeductCoins: (AgentSummaryDto) -> Unit,
    onViewTransactions: (AgentSummaryDto) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredAgents = agents.filter {
        searchQuery.isBlank() ||
                it.agentId.contains(searchQuery, ignoreCase = true) ||
                it.agentName.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .testTag("admin_coins_directory_view")
    ) {
        // Treasury Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, AccentGold.copy(alpha = 0.4f))))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("ADMIN TREASURY BALANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${formatCoins(adminBalance)} COINS", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AccentGold)
                    Text("Authoritative server virtual coin pool", fontSize = 11.sp, color = TextMuted)
                }
                IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_admin_treasury_btn")) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = AccentGold)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by Agent ID or Name...", fontSize = 13.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = TextMuted) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("admin_coins_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AccentGold,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "COINS → AGENT MANAGEMENT (${filteredAgents.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else if (filteredAgents.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(CardBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No agents found", color = TextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredAgents) { agent ->
                    val balance = accountBalances[agent.id] ?: 0L
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("agent_coin_row_${agent.agentId}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, CardBorder)))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = AccentGold.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = AccentGold, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = agent.agentName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                        Text(text = "ID: ${agent.agentId}", fontSize = 11.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (agent.status == "ACTIVE") SuccessGreen.copy(alpha = 0.2f) else DangerRed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = agent.status,
                                        color = if (agent.status == "ACTIVE") SuccessGreen else DangerRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = CardBorder)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Current Coin Balance", fontSize = 11.sp, color = TextMuted)
                                    Text(
                                        text = "${formatCoins(balance)} coins",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AccentGold
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Subordinate Users", fontSize = 11.sp, color = TextMuted)
                                    Text("${agent.userCount}", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onTransferCoins(agent) },
                                    enabled = agent.status == "ACTIVE",
                                    colors = ButtonDefaults.buttonColors(containerColor = AccentGold, contentColor = Color.Black),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("transfer_to_agent_${agent.agentId}")
                                ) {
                                    Text("Transfer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onDeductCoins(agent) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("deduct_from_agent_${agent.agentId}")
                                ) {
                                    Text("Deduct", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onViewTransactions(agent) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("view_tx_agent_${agent.agentId}")
                                ) {
                                    Text("Transactions", fontSize = 11.sp)
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
 * Agent Navigation: Coins -> Users.
 * Lists all subordinate users with User ID, Display Name, Status, Current Coin Balance, Last Activity, and Actions.
 */
@Composable
fun AgentCoinsDirectoryView(
    agentBalance: Long,
    users: List<UserSummaryDto>,
    accountBalances: Map<String, Long>,
    isLoading: Boolean,
    onRefresh: () -> Unit,
    onTransferCoins: (UserSummaryDto) -> Unit,
    onDeductCoins: (UserSummaryDto) -> Unit,
    onViewTransactions: (UserSummaryDto) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    val filteredUsers = users.filter {
        searchQuery.isBlank() ||
                it.userId.contains(searchQuery, ignoreCase = true) ||
                it.displayName.contains(searchQuery, ignoreCase = true)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .testTag("agent_coins_directory_view")
    ) {
        // Agent Working Balance Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(14.dp),
            colors = CardDefaults.cardColors(containerColor = CardBg),
            border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, AgentBlue.copy(alpha = 0.4f))))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("AGENT ALLOCATED COIN BALANCE", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = TextMuted)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("${formatCoins(agentBalance)} COINS", fontSize = 22.sp, fontWeight = FontWeight.Bold, color = AgentBlue)
                    Text("Available to transfer to subordinated users", fontSize = 11.sp, color = TextMuted)
                }
                IconButton(onClick = onRefresh, modifier = Modifier.testTag("refresh_agent_working_coins_btn")) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = "Refresh", tint = AgentBlue)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by User ID or Name...", fontSize = 13.sp) },
            leadingIcon = { Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = TextMuted) },
            singleLine = true,
            modifier = Modifier
                .fillMaxWidth()
                .testTag("agent_coins_search_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AgentBlue,
                unfocusedBorderColor = CardBorder,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White,
                focusedContainerColor = CardBg,
                unfocusedContainerColor = CardBg
            )
        )

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "COINS → USERS (${filteredUsers.size})",
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = TextMuted,
            letterSpacing = 1.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = AgentBlue)
            }
        } else if (filteredUsers.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(160.dp)
                    .background(CardBg, RoundedCornerShape(10.dp)),
                contentAlignment = Alignment.Center
            ) {
                Text("No users found", color = TextMuted, fontSize = 14.sp)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredUsers) { user ->
                    val balance = accountBalances[user.id] ?: 0L
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("user_coin_row_${user.userId}"),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = CardBg),
                        border = CardDefaults.outlinedCardBorder().copy(brush = Brush.horizontalGradient(listOf(CardBorder, CardBorder)))
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Surface(
                                        shape = CircleShape,
                                        color = AgentBlue.copy(alpha = 0.15f),
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = AgentBlue, modifier = Modifier.size(20.dp))
                                        }
                                    }
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Column {
                                        Text(text = user.displayName, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = Color.White)
                                        Text(text = "ID: ${user.userId}", fontSize = 11.sp, color = TextMuted, fontFamily = FontFamily.Monospace)
                                    }
                                }
                                Surface(
                                    shape = RoundedCornerShape(6.dp),
                                    color = if (user.status == "ACTIVE") SuccessGreen.copy(alpha = 0.2f) else DangerRed.copy(alpha = 0.2f)
                                ) {
                                    Text(
                                        text = user.status,
                                        color = if (user.status == "ACTIVE") SuccessGreen else DangerRed,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(10.dp))
                            HorizontalDivider(color = CardBorder)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text("Current Coin Balance", fontSize = 11.sp, color = TextMuted)
                                    Text(
                                        text = "${formatCoins(balance)} coins",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = AgentBlue
                                    )
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Last Activity", fontSize = 11.sp, color = TextMuted)
                                    Text(formatLedgerDate(user.lastLoginAt ?: user.createdAt), fontSize = 11.sp, color = Color.White)
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            // Action Buttons
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Button(
                                    onClick = { onTransferCoins(user) },
                                    enabled = user.status == "ACTIVE",
                                    colors = ButtonDefaults.buttonColors(containerColor = AgentBlue, contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("transfer_to_user_${user.userId}")
                                ) {
                                    Text("Transfer", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onDeductCoins(user) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = DangerRed),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("deduct_from_user_${user.userId}")
                                ) {
                                    Text("Deduct", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                }

                                OutlinedButton(
                                    onClick = { onViewTransactions(user) },
                                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                                    shape = RoundedCornerShape(8.dp),
                                    modifier = Modifier
                                        .weight(1.2f)
                                        .testTag("view_tx_user_${user.userId}")
                                ) {
                                    Text("Transactions", fontSize = 11.sp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

