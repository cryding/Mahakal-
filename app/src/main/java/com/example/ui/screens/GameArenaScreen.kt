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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
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
import com.example.data.local.entity.GameEntity
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.BorderStroke
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
fun GameArenaView(
    currentUser: UserEntity,
    viewModel: MahakalViewModel,
    games: List<GameEntity>
) {
    var selectedGame by remember(games) { mutableStateOf(games.firstOrNull()) }
    var selectedOption by remember { mutableStateOf("") }
    var coinStake by remember { mutableStateOf("500") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Text(
                text = "MAHAKAL LIVE GAME ARENA",
                style = MaterialTheme.typography.titleLarge,
                color = GoldPrimary,
                fontWeight = FontWeight.Black,
                letterSpacing = 1.sp
            )
            Text(
                text = "Real-time multiplier predictions with immutable blockchain-grade ledger settlement",
                color = TextSecondary,
                fontSize = 12.sp
            )
        }

        // Game selector chips
        item {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(games) { game ->
                    val isSelected = selectedGame?.id == game.id
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) GoldPrimary else DarkSurfaceCard)
                            .border(1.dp, if (isSelected) GoldLight else BorderStroke, RoundedCornerShape(8.dp))
                            .clickable {
                                selectedGame = game
                                selectedOption = ""
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Column {
                            Text(
                                text = game.title,
                                color = if (isSelected) Color.Black else TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${game.multiplier}x Payout",
                                color = if (isSelected) Color.Black.copy(alpha = 0.7f) else GoldLight,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }

        selectedGame?.let { game ->
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
                    shape = RoundedCornerShape(16.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(game.title, fontWeight = FontWeight.Black, fontSize = 18.sp, color = Color.White)
                                Text("Select your winning prediction below", color = TextSecondary, fontSize = 12.sp)
                            }
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(GoldPrimary)
                                    .padding(horizontal = 10.dp, vertical = 4.dp)
                            ) {
                                Text("${game.multiplier}x", color = Color.Black, fontWeight = FontWeight.Black, fontSize = 14.sp)
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Interactive Options selection based on game category
                        val options = when (game.category) {
                            "MATKA_SINGLE" -> listOf("0", "1", "2", "3", "4", "5", "6", "7", "8", "9")
                            "LUCKY_DICE" -> listOf("1", "2", "3", "4", "5", "6")
                            "COLOR_WHEEL" -> listOf("RED", "GREEN", "BLUE")
                            "JODI_PAIR" -> listOf("11", "22", "33", "44", "55", "77", "88", "99", "00")
                            else -> listOf("A", "B", "C", "D")
                        }

                        Text("SELECT AN OPTION:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            options.take(5).forEach { opt ->
                                val isOptSelected = selectedOption == opt
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .height(44.dp)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (isOptSelected) SaffronAccent else DarkSurfaceVariant)
                                        .border(1.dp, if (isOptSelected) GoldPrimary else BorderStroke, RoundedCornerShape(8.dp))
                                        .clickable { selectedOption = opt }
                                        .testTag("option_${opt}"),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = opt,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isOptSelected) Color.White else TextPrimary,
                                        fontSize = 14.sp
                                    )
                                }
                            }
                        }

                        if (options.size > 5) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                options.drop(5).take(5).forEach { opt ->
                                    val isOptSelected = selectedOption == opt
                                    Box(
                                        modifier = Modifier
                                            .weight(1f)
                                            .height(44.dp)
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(if (isOptSelected) SaffronAccent else DarkSurfaceVariant)
                                            .border(1.dp, if (isOptSelected) GoldPrimary else BorderStroke, RoundedCornerShape(8.dp))
                                            .clickable { selectedOption = opt }
                                            .testTag("option_${opt}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = opt,
                                            fontWeight = FontWeight.Bold,
                                            color = if (isOptSelected) Color.White else TextPrimary,
                                            fontSize = 14.sp
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Stake input
                        Text("STAKE AMOUNT (COINS):", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldLight)
                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = coinStake,
                            onValueChange = { coinStake = it },
                            modifier = Modifier.fillMaxWidth().testTag("stake_input"),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = GoldPrimary,
                                unfocusedBorderColor = BorderStroke,
                                focusedTextColor = TextPrimary,
                                unfocusedTextColor = TextPrimary
                            ),
                            leadingIcon = {
                                Icon(Icons.Default.MonetizationOn, contentDescription = null, tint = GoldPrimary)
                            }
                        )

                        Spacer(modifier = Modifier.height(12.dp))

                        val stakeLong = coinStake.toLongOrNull() ?: 0L
                        val potentialPayout = (stakeLong * game.multiplier).toLong()

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("Your Balance: ${formatCoins(currentUser.balance)}", color = TextSecondary, fontSize = 12.sp)
                            Text(
                                text = "Est. Payout: ${NumberFormat.getNumberInstance(Locale.US).format(potentialPayout)} COINS",
                                color = EmeraldGreen,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        Button(
                            onClick = {
                                if (selectedOption.isNotBlank() && stakeLong > 0) {
                                    viewModel.placeGameEntry(game.id, selectedOption, stakeLong)
                                }
                            },
                            enabled = selectedOption.isNotBlank() && stakeLong >= game.minCoins && currentUser.balance >= stakeLong,
                            modifier = Modifier.fillMaxWidth().height(48.dp).testTag("place_entry_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = GoldPrimary,
                                contentColor = Color.Black,
                                disabledContainerColor = DarkSurfaceVariant,
                                disabledContentColor = TextMuted
                            ),
                            shape = RoundedCornerShape(10.dp)
                        ) {
                            Icon(Icons.Default.Casino, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (selectedOption.isBlank()) "SELECT PREDICTION TO ENTER" else "CONFIRM & LOCK ENTRY",
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
