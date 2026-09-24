package com.example.ui.game

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.SportsEsports
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
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.model.EntryStatus
import com.example.backend.model.GameDto
import com.example.backend.model.GameEntryDto
import com.example.backend.model.GameOptionDto
import com.example.backend.model.GameStatus
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun UserGamesScreen(
    gameViewModel: GameViewModel,
    userBalance: Long,
    onBalanceRefresh: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by gameViewModel.uiState.collectAsState()
    var selectedTabIndex by remember { mutableIntStateOf(0) }
    var activeGameForEntry by remember { mutableStateOf<GameDto?>(null) }
    var activeGameForDetails by remember { mutableStateOf<GameDto?>(null) }
    var selectedEntryForDetails by remember { mutableStateOf<GameEntryDto?>(null) }

    val bgDark = Color(0xFF0F141C)
    val cardBg = Color(0xFF18202C)
    val accentGold = Color(0xFFE5A93C)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(bgDark)
            .padding(16.dp)
    ) {
        // Non-monetary virtual coin compliance banner
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF1A2634),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF2A3C50))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = null,
                    tint = accentGold,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Virtual Gameplay Coins Only • Strictly Non-Monetary • No Cash Value",
                    fontSize = 11.sp,
                    color = Color(0xFFB0BEC5),
                    fontWeight = FontWeight.Medium
                )
            }
        }

        // Header with Refresh & Balance
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "PREDICTION ARENA",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
                Text(
                    text = "Available: $userBalance Virtual Coins",
                    fontSize = 12.sp,
                    color = accentGold,
                    fontWeight = FontWeight.SemiBold
                )
            }
            IconButton(
                onClick = {
                    gameViewModel.loadGames()
                    gameViewModel.loadMyEntries()
                    onBalanceRefresh()
                },
                modifier = Modifier.testTag("refresh_games_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh",
                    tint = Color(0xFF7A8B9E)
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
                    Text(text = msg, color = Color(0xFF81C784), fontSize = 12.sp)
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
                    Text(text = err, color = Color(0xFFFF8A80), fontSize = 12.sp)
                }
            }
        }

        // Tabs
        TabRow(
            selectedTabIndex = selectedTabIndex,
            containerColor = Color(0xFF131A24),
            contentColor = accentGold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedTabIndex]),
                    color = accentGold
                )
            },
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Tab(
                selected = selectedTabIndex == 0,
                onClick = { selectedTabIndex = 0 },
                text = { Text("GAMES (${uiState.games.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                modifier = Modifier.testTag("tab_user_games")
            )
            Tab(
                selected = selectedTabIndex == 1,
                onClick = { selectedTabIndex = 1 },
                text = { Text("MY ENTRIES (${uiState.myEntries.size})", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                modifier = Modifier.testTag("tab_user_my_entries")
            )
            val resultsCount = uiState.myEntries.count { it.status == EntryStatus.WON || it.status == EntryStatus.LOST || it.status == EntryStatus.REFUNDED }
            Tab(
                selected = selectedTabIndex == 2,
                onClick = { selectedTabIndex = 2 },
                text = { Text("RESULTS ($resultsCount)", fontWeight = FontWeight.Bold, fontSize = 13.sp) },
                modifier = Modifier.testTag("tab_user_results")
            )
        }

        // Tab Content
        when (selectedTabIndex) {
            0 -> {
                if (uiState.isLoadingGames) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentGold)
                    }
                } else if (uiState.games.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.SportsEsports, contentDescription = null, tint = Color(0xFF7A8B9E), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No active prediction games available.", color = Color(0xFF7A8B9E), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            OutlinedButton(
                                onClick = { gameViewModel.loadGames() },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = accentGold)
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Refresh Arena")
                            }
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        items(uiState.games, key = { it.gameId }) { game ->
                            GameCard(
                                game = game,
                                onParticipate = { activeGameForEntry = game },
                                onViewDetails = { activeGameForDetails = game }
                            )
                        }
                    }
                }
            }
            1 -> {
                if (uiState.isLoadingEntries) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentGold)
                    }
                } else if (uiState.myEntries.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.EmojiEvents, contentDescription = null, tint = Color(0xFF7A8B9E), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("You haven't participated in any games yet.", color = Color(0xFF7A8B9E), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
                                onClick = { selectedTabIndex = 0 },
                                colors = ButtonDefaults.buttonColors(containerColor = accentGold, contentColor = Color.Black)
                            ) {
                                Text("Explore Active Games", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(uiState.myEntries, key = { it.entryId }) { entry ->
                            UserEntryCard(
                                entry = entry,
                                onClick = { selectedEntryForDetails = entry }
                            )
                        }
                    }
                }
            }
            2 -> {
                val resultsList = uiState.myEntries.filter { it.status == EntryStatus.WON || it.status == EntryStatus.LOST || it.status == EntryStatus.REFUNDED }
                if (uiState.isLoadingEntries) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = accentGold)
                    }
                } else if (resultsList.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Schedule, contentDescription = null, tint = Color(0xFF7A8B9E), modifier = Modifier.size(48.dp))
                            Spacer(modifier = Modifier.height(8.dp))
                            Text("No settled prediction results yet.", color = Color(0xFF7A8B9E), fontSize = 14.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Outcomes appear here once games conclude and finalize.", color = Color(0xFF546E7A), fontSize = 12.sp)
                        }
                    }
                } else {
                    LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        items(resultsList, key = { it.entryId }) { entry ->
                            UserEntryCard(
                                entry = entry,
                                onClick = { selectedEntryForDetails = entry }
                            )
                        }
                    }
                }
            }
        }
    }

    // Participate Dialog
    activeGameForEntry?.let { game ->
        GameEntryDialog(
            game = game,
            userBalance = userBalance,
            isSubmitting = uiState.isSubmitting,
            onDismiss = { activeGameForEntry = null },
            onSubmit = { optionId, amount ->
                gameViewModel.submitEntry(game.gameId, optionId, amount) {
                    activeGameForEntry = null
                    onBalanceRefresh()
                }
            }
        )
    }

    // Game Details Dialog
    activeGameForDetails?.let { game ->
        GameDetailsDialog(
            game = game,
            onDismiss = { activeGameForDetails = null },
            onPredict = {
                activeGameForDetails = null
                activeGameForEntry = game
            }
        )
    }

    // Entry Confirmation / Details Dialog
    selectedEntryForDetails?.let { entry ->
        EntryConfirmationDialog(
            entry = entry,
            onDismiss = { selectedEntryForDetails = null }
        )
    }
}

@Composable
fun GameCard(
    game: GameDto,
    onParticipate: () -> Unit,
    onViewDetails: () -> Unit = {}
) {
    val cardBg = Color(0xFF18202C)
    val accentGold = Color(0xFFE5A93C)
    val isOpen = game.status == GameStatus.OPEN
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
            .testTag("game_card_${game.gameId}"),
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
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Text(
                    text = "${game.rewardMultiplier}x Multiplier",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentGold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = game.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            if (game.description.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = game.description,
                    fontSize = 12.sp,
                    color = Color(0xFF90A4AE),
                    lineHeight = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Options summary
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                game.options.take(4).forEach { option ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF222C3D)
                    ) {
                        Text(
                            text = option.displayName,
                            fontSize = 11.sp,
                            color = Color(0xFFCFD8DC),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
                if (game.options.size > 4) {
                    Text(
                        text = "+${game.options.size - 4} more",
                        fontSize = 11.sp,
                        color = Color(0xFF7A8B9E),
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = Color(0xFF263345), thickness = 0.8.dp)
            Spacer(modifier = Modifier.height(10.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Range: ${game.minCoins} - ${game.maxCoins} coins",
                        fontSize = 11.sp,
                        color = Color(0xFF7A8B9E)
                    )
                    val deadlineStr = SimpleDateFormat("hh:mm a", Locale.getDefault()).format(Date(game.entryDeadline))
                    Text(
                        text = "Deadline: $deadlineStr",
                        fontSize = 10.sp,
                        color = Color(0xFFB0BEC5)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedButton(
                        onClick = onViewDetails,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFCFD8DC)),
                        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF37474F)),
                        modifier = Modifier.testTag("details_button_${game.gameId}")
                    ) {
                        Text("Details", fontSize = 12.sp)
                    }

                    Button(
                        onClick = onParticipate,
                        enabled = isOpen,
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = accentGold,
                            contentColor = Color.Black,
                            disabledContainerColor = Color(0xFF2A3648),
                            disabledContentColor = Color(0xFF546E7A)
                        ),
                        modifier = Modifier.testTag("participate_button_${game.gameId}")
                    ) {
                        Text(
                            text = if (isOpen) "Predict Now" else game.status,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun GameEntryDialog(
    game: GameDto,
    userBalance: Long,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onSubmit: (selectedOptionId: String, amount: Long) -> Unit
) {
    var selectedOptionId by remember { mutableStateOf(game.options.firstOrNull()?.optionId ?: "") }
    var coinAmountText by remember { mutableStateOf(game.minCoins.toString()) }
    val accentGold = Color(0xFFE5A93C)
    val parsedAmount = coinAmountText.toLongOrNull() ?: 0L
    val potentialReward = (parsedAmount * game.rewardMultiplier).toLong()

    val isValid = selectedOptionId.isNotEmpty() &&
            parsedAmount >= game.minCoins &&
            parsedAmount <= game.maxCoins &&
            parsedAmount <= userBalance

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF18202C),
        title = {
            Column {
                Text(
                    text = "Place Prediction",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    text = game.title,
                    color = accentGold,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
        },
        text = {
            Column {
                Text(
                    text = "Your Balance: $userBalance Virtual Coins",
                    fontSize = 12.sp,
                    color = Color(0xFF81C784),
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "Select Prediction Outcome:",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFFCFD8DC)
                )
                Spacer(modifier = Modifier.height(6.dp))

                // Options list
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    game.options.forEach { opt ->
                        val isSelected = opt.optionId == selectedOptionId
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) Color(0xFF233245) else Color(0xFF131A24),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) accentGold else Color(0xFF263345)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedOptionId = opt.optionId }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
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
                                    fontSize = 13.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Virtual Coins Input
                OutlinedTextField(
                    value = coinAmountText,
                    onValueChange = { coinAmountText = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Virtual Coins (${game.minCoins} - ${game.maxCoins})") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = accentGold,
                        unfocusedBorderColor = Color(0xFF37474F)
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("entry_coins_input")
                )

                // Quick preset buttons
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(10L, 50L, 100L).filter { it in game.minCoins..game.maxCoins }.forEach { preset ->
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = Color(0xFF222C3D),
                            modifier = Modifier.clickable { coinAmountText = preset.toString() }
                        ) {
                            Text(
                                text = "+$preset",
                                fontSize = 11.sp,
                                color = Color(0xFFCFD8DC),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Potential Reward
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF131A24),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Est. Reward (${game.rewardMultiplier}x):", fontSize = 12.sp, color = Color(0xFF7A8B9E))
                            Text("$potentialReward Virtual Coins", fontSize = 12.sp, color = accentGold, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Non-monetary virtual gameplay points only.",
                            fontSize = 10.sp,
                            color = Color(0xFF546E7A)
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(selectedOptionId, parsedAmount) },
                enabled = isValid && !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = accentGold, contentColor = Color.Black),
                modifier = Modifier.testTag("confirm_entry_button")
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Color.Black)
                } else {
                    Text("Confirm Prediction", fontWeight = FontWeight.Bold)
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
fun UserEntryCard(
    entry: GameEntryDto,
    onClick: () -> Unit = {}
) {
    val cardBg = Color(0xFF18202C)
    val statusColor = when (entry.status) {
        EntryStatus.WON -> Color(0xFF66BB6A)
        EntryStatus.LOST -> Color(0xFF78909C)
        EntryStatus.REFUNDED -> Color(0xFFFFA726)
        EntryStatus.CANCELLED -> Color(0xFFEF5350)
        else -> Color(0xFF42A5F5)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .testTag("user_entry_card_${entry.entryId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = cardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF263345))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.gameTitle ?: "Prediction Game",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "Choice: ${entry.selectedOptionName ?: entry.selectedOptionId}",
                    fontSize = 12.sp,
                    color = Color(0xFFE5A93C),
                    fontWeight = FontWeight.SemiBold
                )
                val dateStr = SimpleDateFormat("dd MMM, hh:mm a", Locale.getDefault()).format(Date(entry.createdAt))
                Text(
                    text = dateStr,
                    fontSize = 10.sp,
                    color = Color(0xFF7A8B9E)
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = entry.status,
                        color = statusColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${entry.virtualCoinAmount} Coins",
                    fontSize = 12.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )

                if (entry.status == EntryStatus.WON && entry.rewardAmount != null) {
                    Text(
                        text = "+${entry.rewardAmount} Won",
                        fontSize = 11.sp,
                        color = Color(0xFF66BB6A),
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}

@Composable
fun GameDetailsDialog(
    game: GameDto,
    onDismiss: () -> Unit,
    onPredict: () -> Unit
) {
    val accentGold = Color(0xFFE5A93C)
    val isOpen = game.status == GameStatus.OPEN
    val statusColor = when (game.status) {
        GameStatus.OPEN -> Color(0xFF66BB6A)
        GameStatus.SCHEDULED -> Color(0xFF42A5F5)
        GameStatus.CLOSED -> Color(0xFFFFA726)
        GameStatus.RESULT_FINALIZED -> Color(0xFFAB47BC)
        GameStatus.CANCELLED -> Color(0xFFEF5350)
        else -> Color(0xFF78909C)
    }

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
                    text = "Game Details",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
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
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = game.title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )

                if (game.description.isNotEmpty()) {
                    Text(
                        text = game.description,
                        color = Color(0xFF90A4AE),
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = Color(0xFF1C2433),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Multiplier", color = Color(0xFF7A8B9E), fontSize = 12.sp)
                            Text("${game.rewardMultiplier}x", color = accentGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Allowed Coin Range", color = Color(0xFF7A8B9E), fontSize = 12.sp)
                            Text("${game.minCoins} - ${game.maxCoins} coins", color = Color.White, fontSize = 12.sp)
                        }
                        val deadlineStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(game.entryDeadline))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Entry Deadline", color = Color(0xFF7A8B9E), fontSize = 12.sp)
                            Text(deadlineStr, color = Color(0xFFB0BEC5), fontSize = 12.sp)
                        }
                        if (game.result != null) {
                            val settledStr = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(game.result.finalizedAt))
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Finalized At", color = Color(0xFF7A8B9E), fontSize = 12.sp)
                                Text(settledStr, color = Color(0xFFB0BEC5), fontSize = 12.sp)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Winning Outcome", color = Color(0xFF7A8B9E), fontSize = 12.sp)
                                val winOptName = game.result.winningOptionName ?: game.options.firstOrNull { it.optionId == game.result.winningOptionId }?.displayName ?: game.result.winningOptionId
                                Text(winOptName, color = Color(0xFF66BB6A), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }

                Text(
                    text = "Options Available (${game.options.size})",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )

                val winningId = game.result?.winningOptionId
                game.options.forEach { option ->
                    val isWinning = option.optionId == winningId
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = if (isWinning) Color(0xFF1B3B2B) else Color(0xFF18202C),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isWinning) Color(0xFF66BB6A) else Color(0xFF263345)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = option.displayName,
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                            if (isWinning) {
                                Text("WINNER", color = Color(0xFF66BB6A), fontWeight = FontWeight.Bold, fontSize = 11.sp)
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            if (isOpen) {
                Button(
                    onClick = onPredict,
                    colors = ButtonDefaults.buttonColors(containerColor = accentGold, contentColor = Color.Black),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("dialog_predict_now_button")
                ) {
                    Text("Predict Now", fontWeight = FontWeight.Bold)
                }
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = Color(0xFF7A8B9E))
            }
        }
    )
}

@Composable
fun EntryConfirmationDialog(
    entry: GameEntryDto,
    onDismiss: () -> Unit
) {
    val accentGold = Color(0xFFE5A93C)
    val statusColor = when (entry.status) {
        EntryStatus.WON -> Color(0xFF66BB6A)
        EntryStatus.LOST -> Color(0xFF78909C)
        EntryStatus.REFUNDED -> Color(0xFFFFA726)
        EntryStatus.CANCELLED -> Color(0xFFEF5350)
        else -> Color(0xFF42A5F5)
    }

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
                    text = "Prediction Receipt",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = statusColor.copy(alpha = 0.2f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, statusColor.copy(alpha = 0.5f))
                ) {
                    Text(
                        text = entry.status,
                        color = statusColor,
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
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Game", color = Color(0xFF7A8B9E), fontSize = 12.sp)
                            Text(entry.gameTitle ?: "Prediction Game", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Selected Prediction", color = Color(0xFF7A8B9E), fontSize = 12.sp)
                            Text(entry.selectedOptionName ?: entry.selectedOptionId, color = accentGold, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Coins Placed", color = Color(0xFF7A8B9E), fontSize = 12.sp)
                            Text("${entry.virtualCoinAmount} Coins", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                        val placedDate = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault()).format(Date(entry.createdAt))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Timestamp", color = Color(0xFF7A8B9E), fontSize = 12.sp)
                            Text(placedDate, color = Color(0xFFB0BEC5), fontSize = 11.sp)
                        }
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                            Text("Entry ID", color = Color(0xFF7A8B9E), fontSize = 12.sp)
                            Text(entry.entryId.take(12) + "...", color = Color(0xFF546E7A), fontSize = 11.sp)
                        }
                        if (entry.status == EntryStatus.WON && entry.rewardAmount != null) {
                            HorizontalDivider(color = Color(0xFF263345), thickness = 0.8.dp)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Reward Claimed", color = Color(0xFF66BB6A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                                Text("+${entry.rewardAmount} Coins", color = Color(0xFF66BB6A), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color(0xFF18202C),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Authoritative server-managed entry. Outcomes are evaluated and credited cryptographically by the platform engine upon finalization.",
                        color = Color(0xFF7A8B9E),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(10.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                colors = ButtonDefaults.buttonColors(containerColor = accentGold, contentColor = Color.Black),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier.testTag("dialog_close_receipt_button")
            ) {
                Text("Done", fontWeight = FontWeight.Bold)
            }
        }
    )
}
