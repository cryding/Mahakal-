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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Logout
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.UserEntity
import com.example.ui.theme.BorderStroke
import com.example.ui.theme.CrimsonRed
import com.example.ui.theme.DarkBackground
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

@Composable
fun LoginScreen(viewModel: MahakalViewModel) {
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBackground)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = DarkSurfaceCard),
            shape = RoundedCornerShape(20.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, BorderStroke)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(listOf(GoldPrimary, SaffronAccent))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "M",
                        color = Color.Black,
                        fontWeight = FontWeight.Black,
                        fontSize = 34.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "MAHAKAL",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Black,
                    color = GoldPrimary,
                    letterSpacing = 3.sp
                )

                Text(
                    text = "Hierarchical Virtual Coin Management",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(24.dp))

                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    label = { Text("Username") },
                    leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = GoldPrimary) },
                    modifier = Modifier.fillMaxWidth().testTag("username_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = BorderStroke,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = password,
                    onValueChange = { password = it },
                    label = { Text("Password") },
                    leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = GoldPrimary) },
                    visualTransformation = PasswordVisualTransformation(),
                    modifier = Modifier.fillMaxWidth().testTag("password_input"),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = GoldPrimary,
                        unfocusedBorderColor = BorderStroke,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    )
                )

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (username.isNotBlank() && password.isNotBlank()) {
                            viewModel.login(username, password)
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(48.dp).testTag("login_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = GoldPrimary, contentColor = Color.Black),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("SECURE SIGN IN", fontWeight = FontWeight.Bold, fontSize = 14.sp)
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "QUICK DEMO ROLE SWITCHER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = GoldLight,
                    letterSpacing = 1.sp
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickRoleButton(
                        label = "SUPER ADMIN",
                        role = "ADMIN",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.switchAccount("admin_master") }
                    )
                    QuickRoleButton(
                        label = "DELHI AGENT",
                        role = "AGENT",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.switchAccount("agent_delhi") }
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickRoleButton(
                        label = "PLAYER RAJ",
                        role = "USER",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.switchAccount("player_raj") }
                    )
                    QuickRoleButton(
                        label = "PLAYER VIKRAM",
                        role = "USER",
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.switchAccount("player_vikram") }
                    )
                }
            }
        }
    }
}

@Composable
fun QuickRoleButton(
    label: String,
    role: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val borderColor = when (role) {
        "ADMIN" -> CrimsonRed
        "AGENT" -> GoldPrimary
        else -> EmeraldGreen
    }

    Box(
        modifier = modifier
            .height(38.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(DarkSurfaceVariant)
            .border(1.dp, borderColor.copy(alpha = 0.6f), RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(horizontal = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

@Composable
fun ProfileDialog(
    user: UserEntity,
    allAccounts: List<UserEntity>,
    onDismiss: () -> Unit,
    onSwitchUser: (String) -> Unit,
    onLogout: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = DarkSurfaceCard,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Shield, contentDescription = null, tint = GoldPrimary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Account & RBAC Role", color = GoldPrimary, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(text = "Logged in as: ${user.fullName}", color = TextPrimary, fontWeight = FontWeight.Bold)
                Text(text = "Username: @${user.username}", color = TextSecondary, fontSize = 13.sp)
                Text(text = "Role: ${user.role}", color = GoldLight, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(text = "Coin Balance: ${formatCoins(user.balance)}", color = Color.White, fontWeight = FontWeight.Black)

                Spacer(modifier = Modifier.height(10.dp))
                Text(text = "SWITCH DEMO SESSION:", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = GoldLight)

                allAccounts.forEach { acc ->
                    val isCurrent = acc.id == user.id
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSwitchUser(acc.id)
                                onDismiss()
                            },
                        colors = CardDefaults.cardColors(containerColor = if (isCurrent) GoldDark.copy(alpha = 0.3f) else DarkSurfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(acc.fullName, color = TextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                Text("${acc.role} • ${acc.username}", color = TextSecondary, fontSize = 10.sp)
                            }
                            RoleBadge(role = acc.role)
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onLogout()
                    onDismiss()
                },
                colors = ButtonDefaults.buttonColors(containerColor = CrimsonRed, contentColor = Color.White)
            ) {
                Icon(Icons.Default.Logout, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("LOGOUT")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("CLOSE", color = TextSecondary)
            }
        }
    )
}
