package com.example.ui.screens

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MahakalViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AgentDashboardView(
    agent: UserEntity,
    viewModel: MahakalViewModel,
    downlineUsers: List<UserEntity>
) {
    var showOnboardDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf<UserEntity?>(null) }
    var showDeductDialog by remember { mutableStateOf<UserEntity?>(null) }
    var showUserDetailsDialog by remember { mutableStateOf<UserEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        BalanceCard(
            user = agent,
            onTransferCoins = {
                if (downlineUsers.isNotEmpty()) {
                    showTransferDialog = downlineUsers.first()
                }
            }
        )

        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "MY SUBORDINATED USERS (${downlineUsers.size})",
                            style = MaterialTheme.typography.titleMedium,
                            color = GoldLight,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Isolated downline under Regional Agent @${agent.username}",
                            color = TextSecondary,
                            fontSize = 11.sp
                        )
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = { viewModel.syncFromBackend("AGENT") }) {
                            Icon(Icons.Default.CloudSync, contentDescription = "Sync", tint = GoldPrimary)
                        }
                        Button(
                            onClick = { showOnboardDialog = true },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.testTag("onboard_user_fab")
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ONBOARD", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            items(downlineUsers) { player ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { showUserDetailsDialog = player },
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = player.fullName,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 16.sp
                                )
                                Text(
                                    text = "@${player.username}",
                                    color = TextSecondary,
                                    fontSize = 12.sp
                                )
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (player.status == "ACTIVE") EmeraldGreen.copy(alpha = 0.2f) else CrimsonRed.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = player.status,
                                    color = if (player.status == "ACTIVE") EmeraldGreen else CrimsonRed,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("COIN BALANCE", fontSize = 10.sp, color = TextMuted)
                                Text(
                                    text = NumberFormat.getNumberInstance(Locale.US).format(player.balance) + " COINS",
                                    color = GoldPrimary,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { showUserDetailsDialog = player },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground, contentColor = TextSecondary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("INFO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { showTransferDialog = player },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground, contentColor = GoldPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("CREDIT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { showDeductDialog = player },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground, contentColor = CrimsonRed),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("DEDUCT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Onboard User Dialog
    if (showOnboardDialog) {
        var username by remember { mutableStateOf("") }
        var fullName by remember { mutableStateOf("") }
        var initialCoins by remember { mutableStateOf("10000") }

        AlertDialog(
            onDismissRequest = { showOnboardDialog = false },
            containerColor = DarkSurfaceCard,
            title = { Text("Register Player Under Agent", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("The initial coins will be deducted from your Agent balance.", color = TextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Player Login Username") },
                        modifier = Modifier.fillMaxWidth().testTag("onboard_username_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = fullName,
                        onValueChange = { fullName = it },
                        label = { Text("Full Name") },
                        modifier = Modifier.fillMaxWidth().testTag("onboard_fullname_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = initialCoins,
                        onValueChange = { initialCoins = it },
                        label = { Text("Coins to Credit") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("onboard_coins_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val coins = initialCoins.toLongOrNull() ?: 0L
                        if (username.isNotBlank() && coins > 0) {
                            viewModel.createUserUnderAgent(username, fullName, coins)
                            showOnboardDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_onboard_button")
                ) {
                    Text("ONBOARD", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showOnboardDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // Transfer Dialog
    showTransferDialog?.let { targetUser ->
        var transferAmount by remember { mutableStateOf("5000") }
        var notes by remember { mutableStateOf("Player coin reload") }

        AlertDialog(
            onDismissRequest = { showTransferDialog = null },
            containerColor = DarkSurfaceCard,
            title = { Text("Credit Coins to Player", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Player: ${targetUser.fullName} (${targetUser.username})", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = transferAmount,
                        onValueChange = { transferAmount = it },
                        label = { Text("Amount of Coins") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = notes,
                        onValueChange = { notes = it },
                        label = { Text("Note") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = transferAmount.toLongOrNull() ?: 0L
                        if (amt > 0) {
                            viewModel.transferCoins(targetUser.id, amt, notes)
                            showTransferDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("CONFIRM CREDIT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // Deduct Dialog
    showDeductDialog?.let { targetUser ->
        var deductAmount by remember { mutableStateOf("1000") }
        var reason by remember { mutableStateOf("Agent Balance Recall") }
        AlertDialog(
            onDismissRequest = { showDeductDialog = null },
            containerColor = DarkSurfaceCard,
            title = { Text("Deduct Coins from Player", color = CrimsonRed, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Player: ${targetUser.fullName} (@${targetUser.username})", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Current Balance: ${formatCoins(targetUser.balance)}", color = GoldLight, fontSize = 12.sp)
                    OutlinedTextField(
                        value = deductAmount,
                        onValueChange = { deductAmount = it },
                        label = { Text("Amount of Coins") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("agent_deduct_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonRed,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = reason,
                        onValueChange = { reason = it },
                        label = { Text("Reason / Note") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CrimsonRed,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val amt = deductAmount.toLongOrNull() ?: 0L
                        if (amt > 0) {
                            viewModel.deductCoins(targetUser.id, amt, reason)
                            showDeductDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White)
                ) {
                    Text("CONFIRM DEDUCT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeductDialog = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // User Details Dialog
    showUserDetailsDialog?.let { targetUser ->
        UserDetailsDialog(
            user = targetUser,
            onDismiss = { showUserDetailsDialog = null }
        )
    }
}
