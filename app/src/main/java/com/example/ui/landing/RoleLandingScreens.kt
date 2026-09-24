package com.example.ui.landing

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ExitToApp
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SupportAgent
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.model.UserProfileDto
import com.example.ui.game.GameViewModel
import com.example.ui.game.UserGamesScreen
import com.example.ui.notification.NotificationViewModel
import com.example.ui.wallet.TransactionDetailDialog
import com.example.ui.wallet.TransactionItemRow
import com.example.ui.wallet.WalletBalanceCard
import com.example.ui.wallet.WalletViewModel

@Composable
fun AdminLandingScreen(
    user: UserProfileDto,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    BaseRoleLandingScreen(
        title = "ADMIN CONSOLE",
        roleName = "ADMINISTRATOR",
        roleColor = Color(0xFFE5A93C),
        roleIcon = Icons.Default.AdminPanelSettings,
        user = user,
        phaseNote = "Phase 2 Active: Authentication & RBAC established. Agent creation & coin management modules will activate in Phase 3 & 5.",
        onLogout = onLogout,
        modifier = modifier
    )
}

@Composable
fun AgentLandingScreen(
    user: UserProfileDto,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    BaseRoleLandingScreen(
        title = "AGENT PORTAL",
        roleName = "AGENT",
        roleColor = Color(0xFF42A5F5),
        roleIcon = Icons.Default.SupportAgent,
        user = user,
        phaseNote = "Phase 2 Active: Agent authenticated. Subordinated User creation and wallet delegation will activate in Phase 4 & 6.",
        onLogout = onLogout,
        modifier = modifier
    )
}

@Composable
fun UserLandingScreen(
    user: UserProfileDto,
    walletViewModel: WalletViewModel? = null,
    gameViewModel: GameViewModel? = null,
    notificationViewModel: NotificationViewModel? = null,
    onOpenNotifications: (() -> Unit)? = null,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val walletState = walletViewModel?.uiState?.collectAsState()?.value
    val unreadNotifCount = notificationViewModel?.unreadCount?.collectAsState()?.value ?: 0
    var selectedTx by remember { mutableStateOf<com.example.backend.model.TransactionDto?>(null) }
    var userActiveTab by remember { mutableIntStateOf(0) } // 0: Predictions, 1: Wallet Ledger, 2: Security & Info

    val bgDark = Color(0xFF0F141C)
    val cardBg = Color(0xFF18202C)
    val roleColor = Color(0xFF66BB6A)
    val accentGold = Color(0xFFE5A93C)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgDark, Color(0xFF0A0D14))
                )
            )
            .padding(16.dp)
    ) {
        // Header Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = CircleShape,
                    color = roleColor.copy(alpha = 0.15f),
                    modifier = Modifier.size(40.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Player",
                            tint = roleColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.size(10.dp))
                Column {
                    Text(
                        text = user.fullName,
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "PLAYER @${user.loginId}",
                        color = Color(0xFF7A8B9E),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (onOpenNotifications != null) {
                    BadgedBox(
                        badge = {
                            if (unreadNotifCount > 0) {
                                Badge(
                                    containerColor = Color(0xFFE53935),
                                    contentColor = Color.White
                                ) {
                                    Text(
                                        text = if (unreadNotifCount > 99) "99+" else unreadNotifCount.toString(),
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }
                    ) {
                        IconButton(
                            onClick = onOpenNotifications,
                            modifier = Modifier
                                .size(36.dp)
                                .testTag("user_notifications_btn")
                        ) {
                            Icon(
                                imageVector = if (unreadNotifCount > 0) Icons.Default.NotificationsActive else Icons.Default.Notifications,
                                contentDescription = "Notifications",
                                tint = if (unreadNotifCount > 0) accentGold else Color(0xFF7A8B9E),
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.size(8.dp))
                }

                Button(
                    onClick = onLogout,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF222C3D)),
                    shape = RoundedCornerShape(8.dp),
                    modifier = Modifier.testTag("logout_button")
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                        contentDescription = "Logout",
                        tint = Color(0xFFFF8A80),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.size(6.dp))
                    Text("Logout", color = Color(0xFFFF8A80), fontSize = 12.sp)
                }
            }
        }

        // Navigation Tabs for User
        TabRow(
            selectedTabIndex = userActiveTab,
            containerColor = Color(0xFF131A24),
            contentColor = accentGold,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[userActiveTab]),
                    color = accentGold
                )
            },
            modifier = Modifier.padding(bottom = 12.dp)
        ) {
            Tab(
                selected = userActiveTab == 0,
                onClick = { userActiveTab = 0 },
                text = { Text("GAMES", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.testTag("user_tab_games")
            )
            Tab(
                selected = userActiveTab == 1,
                onClick = { userActiveTab = 1 },
                text = { Text("WALLET", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.testTag("user_tab_wallet")
            )
            Tab(
                selected = userActiveTab == 2,
                onClick = { userActiveTab = 2 },
                text = { Text("PROFILE", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.testTag("user_tab_profile")
            )
            Tab(
                selected = userActiveTab == 3,
                onClick = { userActiveTab = 3 },
                text = { Text("SECURITY", fontWeight = FontWeight.Bold, fontSize = 12.sp) },
                modifier = Modifier.testTag("user_tab_security")
            )
        }

        when (userActiveTab) {
            0 -> {
                if (gameViewModel != null) {
                    UserGamesScreen(
                        gameViewModel = gameViewModel,
                        userBalance = walletState?.myBalance?.balance ?: 0L,
                        onBalanceRefresh = { walletViewModel?.loadBalance() },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
            1 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Phase 5: Virtual Coin Wallet Card
                    WalletBalanceCard(
                        balance = walletState?.myBalance?.balance ?: 0L,
                        isLoading = walletState?.isLoadingBalance ?: false,
                        role = "USER",
                        onRefresh = { walletViewModel?.loadBalance() },
                        modifier = Modifier.padding(bottom = 16.dp)
                    )

                    // Personal Transaction Ledger Card
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 16.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "MY TRANSACTION HISTORY",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFE5A93C),
                                    letterSpacing = 1.sp
                                )
                                Text(
                                    text = "${walletState?.totalTransactionsCount ?: 0} Entries",
                                    fontSize = 11.sp,
                                    color = Color(0xFF7A8B9E)
                                )
                            }

                            Spacer(modifier = Modifier.height(12.dp))

                            val transactions = walletState?.transactions ?: emptyList()
                            if (transactions.isEmpty()) {
                                Text(
                                    text = if (walletState?.isLoadingTransactions == true) "Loading ledger..." else "No transactions recorded in your ledger yet.",
                                    fontSize = 12.sp,
                                    color = Color(0xFF7A8B9E),
                                    modifier = Modifier.padding(vertical = 12.dp)
                                )
                            } else {
                                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                    transactions.take(10).forEach { tx ->
                                        TransactionItemRow(
                                            transaction = tx,
                                            onClick = { selectedTx = tx }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            2 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                ) {
                    // Profile Details Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "USER PROFILE",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7A8B9E),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            ProfileField(label = "Full Name", value = user.fullName)
                            ProfileField(label = "Login ID", value = user.loginId)
                            ProfileField(label = "User ID", value = user.id)
                            ProfileField(label = "Role", value = user.role)
                            ProfileField(label = "Account Status", value = user.status, valueColor = Color(0xFF4CAF50))

                            Spacer(modifier = Modifier.height(16.dp))

                            Button(
                                onClick = onLogout,
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF5350)),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.fillMaxWidth().testTag("profile_logout_button")
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ExitToApp, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Sign Out of Device", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                    }
                }
            }
            3 -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    // Account Information & Security Card
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = cardBg)
                    ) {
                        Column(modifier = Modifier.padding(18.dp)) {
                            Text(
                                text = "ACCOUNT SECURITY & POLICY",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF7A8B9E),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            ProfileField(label = "Authentication Type", value = "Argon2id Salted Hash")
                            ProfileField(label = "Session Token", value = "Encrypted JWT (Strict Bearer)")
                            ProfileField(label = "RBAC Scope", value = "ROLE_USER (Authoritative Enforced)")

                            Spacer(modifier = Modifier.height(12.dp))

                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(0xFF222C3D),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = "Non-Monetary Virtual Units Notice",
                                        color = Color.White,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Coins in your wallet are internal non-monetary virtual units. They cannot be purchased, redeemed, converted to fiat currency (INR), or transferred outside the platform.",
                                        color = Color(0xFF7A8B9E),
                                        fontSize = 11.sp,
                                        lineHeight = 16.sp
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        selectedTx?.let { tx ->
            TransactionDetailDialog(
                transaction = tx,
                currentUserRole = "USER",
                onDismiss = { selectedTx = null }
            )
        }
    }
}

@Composable
private fun BaseRoleLandingScreen(
    title: String,
    roleName: String,
    roleColor: Color,
    roleIcon: ImageVector,
    user: UserProfileDto,
    phaseNote: String,
    onLogout: () -> Unit,
    modifier: Modifier = Modifier
) {
    val bgDark = Color(0xFF0F141C)
    val cardBg = Color(0xFF18202C)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgDark, Color(0xFF0A0D14))
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Role Badge Icon
            Surface(
                shape = CircleShape,
                color = roleColor.copy(alpha = 0.15f),
                modifier = Modifier.size(72.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = roleIcon,
                        contentDescription = roleName,
                        tint = roleColor,
                        modifier = Modifier.size(38.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = title,
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                letterSpacing = 2.sp
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = roleColor.copy(alpha = 0.12f),
                modifier = Modifier.padding(top = 8.dp, bottom = 24.dp)
            ) {
                Text(
                    text = "ROLE: $roleName",
                    color = roleColor,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp)
                )
            }

            // Authenticated Session Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = cardBg),
                elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 16.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Session",
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(
                            text = "Authenticated Session Active",
                            color = Color(0xFF81C784),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    ProfileField(label = "Login ID", value = user.loginId)
                    ProfileField(label = "Account Name", value = user.fullName)
                    ProfileField(label = "Internal UUID", value = user.id)
                    ProfileField(label = "Status", value = user.status, valueColor = Color(0xFF4CAF50))

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color(0xFF222C3D),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = "Security Note",
                                    tint = Color(0xFF90A4AE),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.size(6.dp))
                                Text(
                                    text = "Phase 2 Boundary Guard",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFCFD8DC)
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = phaseNote,
                                fontSize = 11.sp,
                                color = Color(0xFF90A4AE),
                                lineHeight = 15.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Logout Button
            Button(
                onClick = onLogout,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF253142),
                    contentColor = Color(0xFFFF8A80)
                ),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 480.dp)
                    .height(48.dp)
                    .testTag("logout_button")
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ExitToApp,
                    contentDescription = "Logout Icon",
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.size(8.dp))
                Text(
                    text = "End Session (Logout)",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp
                )
            }
        }
    }
}

@Composable
private fun ProfileField(
    label: String,
    value: String,
    valueColor: Color = Color.White
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            color = Color(0xFF7A8B9E)
        )
        Text(
            text = value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = valueColor,
            textAlign = TextAlign.End
        )
    }
}
