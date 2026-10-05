package com.example.ui.screens

import androidx.activity.compose.BackHandler
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CloudSync
import androidx.compose.material.icons.filled.ExitToApp
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SwapHoriz
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
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

@Composable
fun AgentDashboardScreen(
    agent: UserEntity,
    viewModel: MahakalViewModel
) {
    val downlineUsers by viewModel.agentUsers.collectAsState()
    val transactions by viewModel.userTransactions.collectAsState()
    var selectedTab by remember { mutableIntStateOf(0) }

    val tabTitles = listOf(
        "1. Dashboard",
        "2. My Users",
        "3. Onboard User",
        "4. Coin Mgmt",
        "5. Transactions",
        "6. Profile"
    )

    BackHandler(enabled = selectedTab != 0) {
        selectedTab = 0
    }

    var showTransferDialog by remember { mutableStateOf<UserEntity?>(null) }
    var showDeductDialog by remember { mutableStateOf<UserEntity?>(null) }
    var showUserDetailsDialog by remember { mutableStateOf<UserEntity?>(null) }

    Scaffold(
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(DarkSurface)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(GoldPrimary.copy(alpha = 0.2f))
                            .border(1.5.dp, GoldPrimary, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.Group, contentDescription = null, tint = GoldPrimary, modifier = Modifier.size(20.dp))
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "MAHAKAL AGENT PORTAL",
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary,
                            fontSize = 15.sp,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "REGIONAL AGENT • @${agent.username}",
                            color = TextSecondary,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { viewModel.syncFromBackend("AGENT") },
                        modifier = Modifier.testTag("agent_sync_button")
                    ) {
                        Icon(Icons.Default.CloudSync, contentDescription = "Sync", tint = GoldPrimary)
                    }
                    IconButton(
                        onClick = { viewModel.logout() },
                        modifier = Modifier.testTag("agent_header_logout_button")
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = "Logout", tint = CrimsonRed)
                    }
                }
            }
        },
        containerColor = DarkBackground
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            BalanceCard(
                user = agent,
                onTransferCoins = {
                    if (downlineUsers.isNotEmpty()) {
                        showTransferDialog = downlineUsers.first()
                    }
                }
            )

            ScrollableTabRow(
                selectedTabIndex = selectedTab,
                containerColor = DarkBackground,
                contentColor = GoldPrimary,
                edgePadding = 12.dp,
                indicator = { tabPositions ->
                    TabRowDefaults.SecondaryIndicator(
                        modifier = Modifier.tabIndicatorOffset(tabPositions[selectedTab]),
                        color = GoldPrimary,
                        height = 3.dp
                    )
                }
            ) {
                tabTitles.forEachIndexed { index, title ->
                    Tab(
                        selected = selectedTab == index,
                        onClick = { selectedTab = index },
                        text = {
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Normal,
                                color = if (selectedTab == index) GoldPrimary else TextMuted
                            )
                        },
                        modifier = Modifier.testTag("agent_tab_$index")
                    )
                }
            }

            when (selectedTab) {
                0 -> AgentOverviewTab(
                    agent = agent,
                    downlineUsers = downlineUsers,
                    onNavigateToTab = { selectedTab = it },
                    onTransferCoins = { if (downlineUsers.isNotEmpty()) showTransferDialog = downlineUsers.first() }
                )
                1 -> AgentMyUsersTab(
                    downlineUsers = downlineUsers,
                    onTransferCoins = { showTransferDialog = it },
                    onDeductCoins = { showDeductDialog = it },
                    onViewDetails = { showUserDetailsDialog = it }
                )
                2 -> AgentOnboardUserTab(
                    agent = agent,
                    viewModel = viewModel,
                    onUserCreated = { selectedTab = 1 }
                )
                3 -> AgentCoinMgmtTab(
                    agent = agent,
                    downlineUsers = downlineUsers,
                    onTransferCoins = { if (downlineUsers.isNotEmpty()) showTransferDialog = downlineUsers.first() },
                    onDeductCoins = { if (downlineUsers.isNotEmpty()) showDeductDialog = downlineUsers.first() }
                )
                4 -> LedgerView(
                    transactions = transactions,
                    currentUserId = agent.id
                )
                5 -> AgentProfileTab(
                    agent = agent,
                    downlineCount = downlineUsers.size,
                    onLogout = { viewModel.logout() },
                    onSyncServer = { viewModel.syncFromBackend("AGENT") }
                )
            }
        }
    }

    // Transfer Modal
    showTransferDialog?.let { targetUser ->
        var transferAmount by remember { mutableStateOf("5000") }
        var notes by remember { mutableStateOf("Downline Credit") }
        AlertDialog(
            onDismissRequest = { showTransferDialog = null },
            containerColor = DarkSurfaceCard,
            title = { Text("Transfer Coins to User", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Recipient: ${targetUser.fullName} (@${targetUser.username})", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = transferAmount,
                        onValueChange = { transferAmount = it },
                        label = { Text("Coin Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("agent_transfer_input"),
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
                        label = { Text("Reference Note") },
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
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_agent_transfer_btn")
                ) {
                    Text("CONFIRM TRANSFER", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // Deduct Modal
    showDeductDialog?.let { targetUser ->
        var deductAmount by remember { mutableStateOf("1000") }
        var reason by remember { mutableStateOf("Agent Balance Recall") }
        AlertDialog(
            onDismissRequest = { showDeductDialog = null },
            containerColor = DarkSurfaceCard,
            title = { Text("Recall Coins from User", color = CrimsonRed, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Target: ${targetUser.fullName} (@${targetUser.username})", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Current User Balance: ${NumberFormat.getNumberInstance(Locale.US).format(targetUser.balance)} Coins", color = GoldLight, fontSize = 12.sp)
                    OutlinedTextField(
                        value = deductAmount,
                        onValueChange = { deductAmount = it },
                        label = { Text("Coin Amount") },
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
                        label = { Text("Reason") },
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
                    colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White),
                    modifier = Modifier.testTag("confirm_agent_deduct_btn")
                ) {
                    Text("CONFIRM RECALL", fontWeight = FontWeight.Bold)
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

@Composable
fun AgentOverviewTab(
    agent: UserEntity,
    downlineUsers: List<UserEntity>,
    onNavigateToTab: (Int) -> Unit,
    onTransferCoins: () -> Unit
) {
    val totalDownlineCoins = downlineUsers.sumOf { it.balance }
    val activeCount = downlineUsers.count { it.status == "ACTIVE" }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "REGIONAL DOWNLINE METRICS",
                style = MaterialTheme.typography.titleMedium,
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("MY USERS", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text("${downlineUsers.size}", fontSize = 22.sp, fontWeight = FontWeight.Black, color = Color.White)
                        Text("$activeCount Active", fontSize = 11.sp, color = EmeraldGreen)
                    }
                }

                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text("DOWNLINE COINS", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = NumberFormat.getNumberInstance(Locale.US).format(totalDownlineCoins),
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Black,
                            color = GoldPrimary
                        )
                        Text("In User Wallets", fontSize = 11.sp, color = TextSecondary)
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("QUICK ACTIONS", style = MaterialTheme.typography.titleSmall, color = GoldLight, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onNavigateToTab(2) },
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("ONBOARD USER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                        Button(
                            onClick = onTransferCoins,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBackground, contentColor = GoldPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.SwapHoriz, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("TRANSFER COINS", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "DOWNLINE RECENT USERS",
                    style = MaterialTheme.typography.titleSmall,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
                TextButton(onClick = { onNavigateToTab(1) }) {
                    Text("VIEW ALL (${downlineUsers.size})", color = GoldPrimary, fontSize = 12.sp)
                }
            }
        }

        if (downlineUsers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text("No users onboarded under your agency yet.", color = TextSecondary, fontSize = 13.sp)
                    }
                }
            }
        } else {
            items(downlineUsers.take(4)) { user ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RoundedCornerShape(10.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(user.fullName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                            Text("@${user.username} • Balance: ${NumberFormat.getNumberInstance(Locale.US).format(user.balance)} Coins", color = TextSecondary, fontSize = 12.sp)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (user.status == "ACTIVE") EmeraldGreen.copy(alpha = 0.2f) else CrimsonRed.copy(alpha = 0.2f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(user.status, color = if (user.status == "ACTIVE") EmeraldGreen else CrimsonRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AgentMyUsersTab(
    downlineUsers: List<UserEntity>,
    onTransferCoins: (UserEntity) -> Unit,
    onDeductCoins: (UserEntity) -> Unit,
    onViewDetails: (UserEntity) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "SUBORDINATED USERS (${downlineUsers.size})",
                style = MaterialTheme.typography.titleMedium,
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )
        }

        if (downlineUsers.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        Text("No users found under your agency. Use 'Onboard User' to add new players.", color = TextSecondary)
                    }
                }
            }
        } else {
            items(downlineUsers) { player ->
                Card(
                    modifier = Modifier.fillMaxWidth().clickable { onViewDetails(player) },
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(player.fullName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 15.sp)
                                Text("@${player.username}", color = TextSecondary, fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(if (player.status == "ACTIVE") EmeraldGreen.copy(alpha = 0.2f) else CrimsonRed.copy(alpha = 0.2f))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(player.status, color = if (player.status == "ACTIVE") EmeraldGreen else CrimsonRed, fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
                                    fontSize = 14.sp
                                )
                            }

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                Button(
                                    onClick = { onViewDetails(player) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground, contentColor = TextSecondary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("INFO", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onTransferCoins(player) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground, contentColor = GoldPrimary),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("CREDIT", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                }

                                Button(
                                    onClick = { onDeductCoins(player) },
                                    colors = ButtonDefaults.buttonColors(containerColor = DarkBackground, contentColor = CrimsonRed),
                                    border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed),
                                    shape = RoundedCornerShape(6.dp)
                                ) {
                                    Text("RECALL", fontSize = 10.sp, fontWeight = FontWeight.Bold)
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
fun AgentOnboardUserTab(
    agent: UserEntity,
    viewModel: MahakalViewModel,
    onUserCreated: () -> Unit
) {
    var username by remember { mutableStateOf("") }
    var fullName by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var initialCoins by remember { mutableStateOf("1000") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "ONBOARD SUBORDINATED PLAYER",
                style = MaterialTheme.typography.titleMedium,
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "New player account will be strictly linked under agent @${agent.username}",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
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
                        value = username,
                        onValueChange = { username = it },
                        label = { Text("Login Username") },
                        modifier = Modifier.fillMaxWidth().testTag("onboard_username_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Temporary Password") },
                        visualTransformation = PasswordVisualTransformation(),
                        modifier = Modifier.fillMaxWidth().testTag("onboard_password_input"),
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
                        label = { Text("Initial Coins (Optional)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Button(
                        onClick = {
                            if (username.isNotBlank() && password.isNotBlank() && fullName.isNotBlank()) {
                                viewModel.createUserUnderAgent(username, fullName, initialCoins.toLongOrNull() ?: 1000L)
                                onUserCreated()
                            }
                        },
                        enabled = username.isNotBlank() && password.isNotBlank() && fullName.isNotBlank(),
                        colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(48.dp).testTag("submit_onboard_user_btn")
                    ) {
                        Icon(Icons.Default.PersonAdd, contentDescription = null)
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("CREATE USER ACCOUNT", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
fun AgentCoinMgmtTab(
    agent: UserEntity,
    downlineUsers: List<UserEntity>,
    onTransferCoins: () -> Unit,
    onDeductCoins: () -> Unit
) {
    val totalDownlineCoins = downlineUsers.sumOf { it.balance }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "AGENT COIN TREASURY & LIQUIDITY",
                style = MaterialTheme.typography.titleMedium,
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Text("AGENT BALANCE IN VAULT", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = NumberFormat.getNumberInstance(Locale.US).format(agent.balance) + " COINS",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Black,
                        color = GoldPrimary
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onTransferCoins,
                            colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("TRANSFER COINS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                        Button(
                            onClick = onDeductCoins,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBackground, contentColor = CrimsonRed),
                            border = androidx.compose.foundation.BorderStroke(1.dp, CrimsonRed),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("RECALL COINS", fontWeight = FontWeight.Bold, fontSize = 11.sp)
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("TOTAL DOWNLINE CIRCULATION", fontSize = 11.sp, color = TextMuted, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = NumberFormat.getNumberInstance(Locale.US).format(totalDownlineCoins) + " COINS",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text("Across ${downlineUsers.size} subordinated user accounts", fontSize = 12.sp, color = TextSecondary)
                }
            }
        }
    }
}

@Composable
fun AgentProfileTab(
    agent: UserEntity,
    downlineCount: Int,
    onLogout: () -> Unit,
    onSyncServer: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "REGIONAL AGENT PROFILE",
                style = MaterialTheme.typography.titleMedium,
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                shape = RoundedCornerShape(14.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("AGENT ID", fontSize = 11.sp, color = TextMuted)
                        Text(agent.id, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("FULL NAME", fontSize = 11.sp, color = TextMuted)
                        Text(agent.fullName, fontSize = 13.sp, color = Color.White, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("LOGIN USERNAME", fontSize = 11.sp, color = TextMuted)
                        Text("@${agent.username}", fontSize = 13.sp, color = GoldLight, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ROLE", fontSize = 11.sp, color = TextMuted)
                        RoleBadge(role = agent.role)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("ACCOUNT STATUS", fontSize = 11.sp, color = TextMuted)
                        Text(agent.status, fontSize = 12.sp, color = EmeraldGreen, fontWeight = FontWeight.Bold)
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("SUBORDINATED USERS", fontSize = 11.sp, color = TextMuted)
                        Text("$downlineCount Players", fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.Bold)
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Button(
                            onClick = onSyncServer,
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBackground, contentColor = GoldPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.CloudSync, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("SYNC SERVER", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = onLogout,
                            colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White),
                            modifier = Modifier.weight(1f).testTag("agent_logout_btn")
                        ) {
                            Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("LOGOUT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }
    }
}
