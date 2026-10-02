package com.example.ui.screens

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
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.LockOpen
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.People
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.GameEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.DarkSurfaceCard
import com.example.ui.theme.DarkSurfaceVariant
import com.example.ui.theme.EmeraldGreen
import com.example.ui.theme.GoldDark
import com.example.ui.theme.GoldLight
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.SaffronAccent
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MahakalViewModel
import java.text.NumberFormat
import java.util.Locale

@Composable
fun AdminDashboardView(
    admin: UserEntity,
    viewModel: MahakalViewModel,
    agents: List<UserEntity>,
    users: List<UserEntity>,
    games: List<GameEntity>
) {
    var selectedTab by remember { mutableIntStateOf(0) }
    val tabTitles = listOf("AGENTS", "GAMES", "OVERVIEW")

    var showMintDialog by remember { mutableStateOf(false) }
    var showCreateAgentDialog by remember { mutableStateOf(false) }
    var showTransferDialog by remember { mutableStateOf<UserEntity?>(null) }
    var showCreateGameDialog by remember { mutableStateOf(false) }
    var showEditGameDialog by remember { mutableStateOf<GameEntity?>(null) }
    var showConfigApiDialog by remember { mutableStateOf<GameEntity?>(null) }
    var showDeclareResultDialog by remember { mutableStateOf<GameEntity?>(null) }

    Column(modifier = Modifier.fillMaxSize()) {
        BalanceCard(
            user = admin,
            onMintCoins = { showMintDialog = true },
            onTransferCoins = {
                if (agents.isNotEmpty()) {
                    showTransferDialog = agents.first()
                }
            }
        )

        // Tab Navigation
        TabRow(
            selectedTabIndex = selectedTab,
            containerColor = DarkBackground,
            contentColor = GoldPrimary,
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
                            fontWeight = if (selectedTab == index) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp,
                            color = if (selectedTab == index) GoldPrimary else TextSecondary
                        )
                    }
                )
            }
        }

        when (selectedTab) {
            0 -> AdminAgentsList(
                agents = agents,
                onAddAgent = { showCreateAgentDialog = true },
                onTransferCoins = { showTransferDialog = it },
                onToggleStatus = { agent -> viewModel.toggleAgentStatus(agent) }
            )
            1 -> AdminGamesList(
                games = games,
                onCreateGame = { showCreateGameDialog = true },
                onEditGame = { showEditGameDialog = it },
                onToggleStatus = { game ->
                    val newSt = if (game.status == "OPEN") "CLOSED" else "OPEN"
                    viewModel.toggleGameStatus(game.id, newSt)
                },
                onConfigApi = { showConfigApiDialog = it },
                onDeclareResult = { showDeclareResultDialog = it }
            )
            2 -> AdminOverviewTab(
                admin = admin,
                agentCount = agents.size,
                userCount = users.size,
                totalCirculating = admin.balance + agents.sumOf { it.balance } + users.sumOf { it.balance }
            )
        }
    }

    // Mint Dialog
    if (showMintDialog) {
        var mintAmount by remember { mutableStateOf("1000000") }
        AlertDialog(
            onDismissRequest = { showMintDialog = false },
            containerColor = DarkSurfaceCard,
            title = { Text("Mint Virtual Coins", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Inject fresh coins into the master platform treasury.", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = mintAmount,
                        onValueChange = { mintAmount = it },
                        label = { Text("Coin Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("mint_amount_input"),
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
                        val amt = mintAmount.toLongOrNull() ?: 0L
                        if (amt > 0) {
                            viewModel.mintTreasuryCoins(amt)
                            showMintDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_mint_button")
                ) {
                    Text("CONFIRM MINT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showMintDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // Appoint Agent Dialog
    if (showCreateAgentDialog) {
        var agentUsername by remember { mutableStateOf("") }
        var agentFullName by remember { mutableStateOf("") }
        var initialCoins by remember { mutableStateOf("500000") }

        AlertDialog(
            onDismissRequest = { showCreateAgentDialog = false },
            containerColor = DarkSurfaceCard,
            title = { Text("Appoint Regional Agent", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Create a new downline regional agent node.", color = TextSecondary, fontSize = 13.sp)
                    OutlinedTextField(
                        value = agentUsername,
                        onValueChange = { agentUsername = it },
                        label = { Text("Agent Login ID") },
                        modifier = Modifier.fillMaxWidth().testTag("agent_username_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = agentFullName,
                        onValueChange = { agentFullName = it },
                        label = { Text("Full Name / Region") },
                        modifier = Modifier.fillMaxWidth().testTag("agent_fullname_input"),
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
                        label = { Text("Initial Coin Allocation") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("agent_coins_input"),
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
                        if (agentUsername.isNotBlank() && coins > 0) {
                            viewModel.createAgent(agentUsername, agentFullName, coins)
                            showCreateAgentDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_create_agent_button")
                ) {
                    Text("APPOINT", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateAgentDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // Transfer Dialog
    showTransferDialog?.let { targetAgent ->
        var transferAmount by remember { mutableStateOf("100000") }
        var notes by remember { mutableStateOf("Treasury replenishment") }

        AlertDialog(
            onDismissRequest = { showTransferDialog = null },
            containerColor = DarkSurfaceCard,
            title = { Text("Transfer Coins to Agent", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Recipient: ${targetAgent.fullName} (${targetAgent.username})", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    OutlinedTextField(
                        value = transferAmount,
                        onValueChange = { transferAmount = it },
                        label = { Text("Amount of Coins") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth().testTag("transfer_amount_input"),
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
                        label = { Text("Ledger Note / Reason") },
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
                            viewModel.transferCoins(targetAgent.id, amt, notes)
                            showTransferDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("TRANSFER", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showTransferDialog = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // Create Custom Game Dialog
    if (showCreateGameDialog) {
        var title by remember { mutableStateOf("") }
        var category by remember { mutableStateOf("CUSTOM_API") }
        var multiplier by remember { mutableStateOf("9.5") }
        var minCoins by remember { mutableStateOf("100") }
        var maxCoins by remember { mutableStateOf("25000") }
        var apiLink by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showCreateGameDialog = false },
            containerColor = DarkSurfaceCard,
            title = { Text("Add Custom Game", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text("Game Title (e.g. Kalyan Night)") },
                        modifier = Modifier.fillMaxWidth().testTag("game_title_input"),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = category,
                        onValueChange = { category = it },
                        label = { Text("Category / Type") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = multiplier,
                        onValueChange = { multiplier = it },
                        label = { Text("Multiplier (e.g. 9.5x)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = minCoins,
                            onValueChange = { minCoins = it },
                            label = { Text("Min Coins") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = BorderStroke,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = maxCoins,
                            onValueChange = { maxCoins = it },
                            label = { Text("Max Coins") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = BorderStroke,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                    OutlinedTextField(
                        value = apiLink,
                        onValueChange = { apiLink = it },
                        label = { Text("Game API / Stream Link (Optional)") },
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
                        val mult = multiplier.toDoubleOrNull() ?: 2.0
                        val min = minCoins.toLongOrNull() ?: 50L
                        val max = maxCoins.toLongOrNull() ?: 10000L
                        if (title.isNotBlank()) {
                            viewModel.createCustomGame(title, category, min, max, mult, apiLink)
                            showCreateGameDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_create_game_button")
                ) {
                    Text("PUBLISH GAME", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateGameDialog = false }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // Edit Game Dialog
    showEditGameDialog?.let { game ->
        var editTitle by remember { mutableStateOf(game.title) }
        var editCategory by remember { mutableStateOf(game.category) }
        var editMultiplier by remember { mutableStateOf(game.multiplier.toString()) }
        var editMinCoins by remember { mutableStateOf(game.minCoins.toString()) }
        var editMaxCoins by remember { mutableStateOf(game.maxCoins.toString()) }
        var editApiLink by remember { mutableStateOf(game.apiLink) }

        AlertDialog(
            onDismissRequest = { showEditGameDialog = null },
            containerColor = DarkSurfaceCard,
            title = { Text("Edit Game Details", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = editTitle,
                        onValueChange = { editTitle = it },
                        label = { Text("Game Title") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    OutlinedTextField(
                        value = editMultiplier,
                        onValueChange = { editMultiplier = it },
                        label = { Text("Multiplier") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = GoldPrimary,
                            unfocusedBorderColor = BorderStroke,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary
                        )
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = editMinCoins,
                            onValueChange = { editMinCoins = it },
                            label = { Text("Min Coins") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = BorderStroke,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                        OutlinedTextField(
                            value = editMaxCoins,
                            onValueChange = { editMaxCoins = it },
                            label = { Text("Max Coins") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = BorderStroke,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            )
                        )
                    }
                    OutlinedTextField(
                        value = editApiLink,
                        onValueChange = { editApiLink = it },
                        label = { Text("API / Stream Link") },
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
                        val mult = editMultiplier.toDoubleOrNull() ?: game.multiplier
                        val min = editMinCoins.toLongOrNull() ?: game.minCoins
                        val max = editMaxCoins.toLongOrNull() ?: game.maxCoins
                        if (editTitle.isNotBlank()) {
                            viewModel.editGame(game.id, editTitle, editCategory, min, max, mult, editApiLink)
                            showEditGameDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("SAVE CHANGES", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditGameDialog = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // Configure API Dialog
    showConfigApiDialog?.let { game ->
        var apiLinkInput by remember { mutableStateOf(game.apiLink) }
        AlertDialog(
            onDismissRequest = { showConfigApiDialog = null },
            containerColor = DarkSurfaceCard,
            title = { Text("Configure Game API / Link", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Configure external contest data source or live stream link for ${game.title}:", color = TextSecondary, fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = apiLinkInput,
                        onValueChange = { apiLinkInput = it },
                        label = { Text("API Endpoint / Stream URL") },
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
                        viewModel.configureGameApi(game.id, apiLinkInput.trim())
                        showConfigApiDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black)
                ) {
                    Text("SAVE API LINK", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfigApiDialog = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }

    // Declare Result Dialog
    showDeclareResultDialog?.let { game ->
        var winningOption by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showDeclareResultDialog = null },
            containerColor = DarkSurfaceCard,
            title = { Text("Declare Result & Pay Winners", color = GoldPrimary, fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Settling: ${game.title}", color = TextPrimary, fontWeight = FontWeight.SemiBold)
                    Text("Multiplier: ${game.multiplier}x", color = GoldLight, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = winningOption,
                        onValueChange = { winningOption = it },
                        label = { Text("Winning Number/Option (e.g. 7 or RED)") },
                        modifier = Modifier.fillMaxWidth().testTag("winning_option_input"),
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
                        if (winningOption.isNotBlank()) {
                            viewModel.finalizeGameResult(game.id, winningOption.trim())
                            showDeclareResultDialog = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = EmeraldGreen, contentColor = Color.Black),
                    modifier = Modifier.testTag("confirm_declare_button")
                ) {
                    Text("FINALIZE & PAY", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeclareResultDialog = null }) {
                    Text("CANCEL", color = TextSecondary)
                }
            }
        )
    }
}

@Composable
fun AdminAgentsList(
    agents: List<UserEntity>,
    onAddAgent: () -> Unit,
    onTransferCoins: (UserEntity) -> Unit,
    onToggleStatus: (UserEntity) -> Unit
) {
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
                Text(
                    text = "APPOINTED AGENTS (${agents.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onAddAgent,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("add_agent_fab")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("NEW AGENT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(agents) { agent ->
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                                text = agent.fullName,
                                fontWeight = FontWeight.Bold,
                                color = Color.White,
                                fontSize = 16.sp
                            )
                            Text(
                                text = "@${agent.username}",
                                color = TextSecondary,
                                fontSize = 12.sp
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (agent.status == "ACTIVE") EmeraldGreen.copy(alpha = 0.2f) else CrimsonRed.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = agent.status,
                                color = if (agent.status == "ACTIVE") EmeraldGreen else CrimsonRed,
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
                            Text("HOLDING BALANCE", fontSize = 10.sp, color = TextMuted)
                            Text(
                                text = NumberFormat.getNumberInstance(Locale.US).format(agent.balance) + " COINS",
                                color = GoldPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = { onTransferCoins(agent) },
                                colors = ButtonDefaults.buttonColors(containerColor = DarkBackground, contentColor = GoldPrimary),
                                border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text("CREDIT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = { onToggleStatus(agent) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (agent.status == "ACTIVE") CrimsonRed.copy(alpha = 0.2f) else EmeraldGreen.copy(alpha = 0.2f),
                                    contentColor = if (agent.status == "ACTIVE") CrimsonRed else EmeraldGreen
                                ),
                                border = androidx.compose.foundation.BorderStroke(1.dp, if (agent.status == "ACTIVE") CrimsonRed else EmeraldGreen),
                                shape = RoundedCornerShape(6.dp)
                            ) {
                                Text(if (agent.status == "ACTIVE") "DISABLE" else "ENABLE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminGamesList(
    games: List<GameEntity>,
    onCreateGame: () -> Unit,
    onEditGame: (GameEntity) -> Unit,
    onToggleStatus: (GameEntity) -> Unit,
    onConfigApi: (GameEntity) -> Unit,
    onDeclareResult: (GameEntity) -> Unit
) {
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
                Text(
                    text = "PLATFORM GAMES (${games.size})",
                    style = MaterialTheme.typography.titleMedium,
                    color = GoldLight,
                    fontWeight = FontWeight.Bold
                )
                Button(
                    onClick = onCreateGame,
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("create_game_button")
                ) {
                    Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("NEW GAME", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        items(games) { game ->
            Card(
                modifier = Modifier.fillMaxWidth(),
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
                        Text(
                            text = game.title,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 16.sp
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (game.status == "OPEN") EmeraldGreen.copy(alpha = 0.2f) else CrimsonRed.copy(alpha = 0.2f))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = game.status,
                                color = if (game.status == "OPEN") EmeraldGreen else CrimsonRed,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("MULTIPLIER: ${game.multiplier}x", color = GoldPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                        Text("LIMIT: ${game.minCoins} - ${game.maxCoins} COINS", color = TextSecondary, fontSize = 12.sp)
                    }

                    if (game.apiLink.isNotBlank()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "API / LINK: ${game.apiLink}",
                            color = SaffronAccent,
                            fontSize = 11.sp,
                            maxLines = 1
                        )
                    }

                    if (game.winningOption != null) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "SETTLED WINNER: [${game.winningOption}]",
                            color = EmeraldGreen,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = { onEditGame(game) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBackground, contentColor = GoldPrimary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, GoldPrimary),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("EDIT", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onToggleStatus(game) },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (game.status == "OPEN") CrimsonRed.copy(alpha = 0.2f) else EmeraldGreen.copy(alpha = 0.2f),
                                contentColor = if (game.status == "OPEN") CrimsonRed else EmeraldGreen
                            ),
                            border = androidx.compose.foundation.BorderStroke(1.dp, if (game.status == "OPEN") CrimsonRed else EmeraldGreen),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (game.status == "OPEN") "DISABLE" else "ENABLE", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }

                        Button(
                            onClick = { onConfigApi(game) },
                            colors = ButtonDefaults.buttonColors(containerColor = DarkBackground, contentColor = TextSecondary),
                            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke),
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("API", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                        }
                    }

                    if (game.status == "OPEN") {
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = { onDeclareResult(game) },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = SaffronAccent, contentColor = Color.White),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("DECLARE RESULT & PAYOUT", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AdminOverviewTab(
    admin: UserEntity,
    agentCount: Int,
    userCount: Int,
    totalCirculating: Long
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "SYSTEM METRICS & PLATFORM HEALTH",
                style = MaterialTheme.typography.titleMedium,
                color = GoldLight,
                fontWeight = FontWeight.Bold
            )
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "CIRCULATING COINS",
                    value = NumberFormat.getNumberInstance(Locale.US).format(totalCirculating),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "TREASURY RESERVE",
                    value = NumberFormat.getNumberInstance(Locale.US).format(admin.balance),
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                MetricCard(
                    title = "ACTIVE AGENTS",
                    value = "$agentCount",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    title = "REGISTERED USERS",
                    value = "$userCount",
                    modifier = Modifier.weight(1f)
                )
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
                    Text("CRYPTOGRAPHIC INTEGRITY GUARANTEE", fontWeight = FontWeight.Bold, color = GoldPrimary, fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Every transaction is recorded with double-entry ledger bookkeeping. Balances cannot be modified without audit trail logging and matching debit-credit counterparty balances.",
                        color = TextSecondary,
                        fontSize = 12.sp,
                        lineHeight = 18.sp
                    )
                }
            }
        }
    }
}

@Composable
fun MetricCard(
    title: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
        shape = RoundedCornerShape(12.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(text = title, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = TextMuted)
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = value, fontSize = 18.sp, fontWeight = FontWeight.Black, color = Color.White)
        }
    }
}
