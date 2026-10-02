package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Casino
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.ReceiptLong
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.MahakalDatabase
import com.example.data.repository.MahakalRepository
import com.example.ui.screens.AdminDashboardView
import com.example.ui.screens.AgentDashboardView
import com.example.ui.screens.AppHeader
import com.example.ui.screens.AuditLogsView
import com.example.ui.screens.GameArenaView
import com.example.ui.screens.LedgerView
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.NotificationsView
import com.example.ui.screens.ProfileDialog
import com.example.ui.screens.UserDashboardView
import com.example.ui.theme.DarkBackground
import com.example.ui.theme.DarkSurface
import com.example.ui.theme.GoldPrimary
import com.example.ui.theme.MahakalTheme
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextSecondary
import com.example.ui.viewmodel.MahakalViewModel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val db = MahakalDatabase.getInstance(applicationContext)
        val repository = MahakalRepository(db)
        val factory = MahakalViewModel.Factory(repository)

        setContent {
            MahakalTheme {
                val viewModel: MahakalViewModel = viewModel(factory = factory)
                MahakalApp(viewModel)
            }
        }
    }
}

@Composable
fun MahakalApp(viewModel: MahakalViewModel) {
    val currentUser by viewModel.currentUser.collectAsState()
    val activeTab by viewModel.activeTab.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showProfileDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (currentUser == null) {
        LoginScreen(viewModel = viewModel)
    } else {
        val user = currentUser!!
        val agents by viewModel.allAgents.collectAsState()
        val allUsers by viewModel.allUsers.collectAsState()
        val agentUsers by viewModel.agentUsers.collectAsState()
        val allGames by viewModel.allGames.collectAsState()
        val activeGames by viewModel.activeGames.collectAsState()
        val userEntries by viewModel.userEntries.collectAsState()
        val transactions by viewModel.userTransactions.collectAsState()
        val auditLogs by viewModel.allAuditLogs.collectAsState()
        val notifications by viewModel.userNotifications.collectAsState()

        val unreadNotifs = notifications.count { !it.isRead }

        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = DarkBackground,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                AppHeader(
                    currentUser = user,
                    unreadCount = unreadNotifs,
                    onOpenNotifications = { viewModel.setTab("NOTIFICATIONS") },
                    onOpenProfile = { showProfileDialog = true }
                )
            },
            bottomBar = {
                NavigationBar(
                    containerColor = DarkSurface,
                    contentColor = GoldPrimary,
                    tonalElevation = 8.dp
                ) {
                    NavigationBarItem(
                        selected = activeTab == "HOME",
                        onClick = { viewModel.setTab("HOME") },
                        icon = { Icon(Icons.Default.Home, contentDescription = "Home") },
                        label = { Text("Home", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_home"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = GoldPrimary,
                            indicatorColor = GoldPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextMuted
                        )
                    )

                    NavigationBarItem(
                        selected = activeTab == "ARENA",
                        onClick = { viewModel.setTab("ARENA") },
                        icon = { Icon(Icons.Default.Casino, contentDescription = "Arena") },
                        label = { Text("Arena", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_arena"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = GoldPrimary,
                            indicatorColor = GoldPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextMuted
                        )
                    )

                    NavigationBarItem(
                        selected = activeTab == "LEDGER",
                        onClick = { viewModel.setTab("LEDGER") },
                        icon = { Icon(Icons.Default.ReceiptLong, contentDescription = "Ledger") },
                        label = { Text("Ledger", fontSize = 11.sp) },
                        modifier = Modifier.testTag("nav_ledger"),
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Color.Black,
                            selectedTextColor = GoldPrimary,
                            indicatorColor = GoldPrimary,
                            unselectedIconColor = TextSecondary,
                            unselectedTextColor = TextMuted
                        )
                    )

                    if (user.role == "ADMIN") {
                        NavigationBarItem(
                            selected = activeTab == "AUDIT",
                            onClick = { viewModel.setTab("AUDIT") },
                            icon = { Icon(Icons.Default.Security, contentDescription = "Audit") },
                            label = { Text("Audit", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_audit"),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = GoldPrimary,
                                indicatorColor = GoldPrimary,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextMuted
                            )
                        )
                    } else {
                        NavigationBarItem(
                            selected = activeTab == "NOTIFICATIONS",
                            onClick = { viewModel.setTab("NOTIFICATIONS") },
                            icon = { Icon(Icons.Default.Notifications, contentDescription = "Alerts") },
                            label = { Text("Alerts", fontSize = 11.sp) },
                            modifier = Modifier.testTag("nav_alerts"),
                            colors = NavigationBarItemDefaults.colors(
                                selectedIconColor = Color.Black,
                                selectedTextColor = GoldPrimary,
                                indicatorColor = GoldPrimary,
                                unselectedIconColor = TextSecondary,
                                unselectedTextColor = TextMuted
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(DarkBackground)
            ) {
                when (activeTab) {
                    "HOME" -> {
                        when (user.role) {
                            "ADMIN" -> AdminDashboardView(
                                admin = user,
                                viewModel = viewModel,
                                agents = agents,
                                users = allUsers,
                                games = allGames
                            )
                            "AGENT" -> AgentDashboardView(
                                agent = user,
                                viewModel = viewModel,
                                downlineUsers = agentUsers
                            )
                            else -> UserDashboardView(
                                user = user,
                                viewModel = viewModel,
                                activeGames = activeGames,
                                userEntries = userEntries,
                                onNavigateToArena = { viewModel.setTab("ARENA") }
                            )
                        }
                    }
                    "ARENA" -> GameArenaView(
                        currentUser = user,
                        viewModel = viewModel,
                        games = activeGames
                    )
                    "LEDGER" -> LedgerView(
                        transactions = transactions,
                        currentUserId = user.id
                    )
                    "AUDIT" -> AuditLogsView(
                        logs = auditLogs
                    )
                    "NOTIFICATIONS" -> NotificationsView(
                        notifications = notifications,
                        onMarkRead = { viewModel.markNotificationRead(it) }
                    )
                }
            }
        }

        if (showProfileDialog) {
            ProfileDialog(
                user = user,
                onDismiss = { showProfileDialog = false },
                onLogout = { viewModel.logout() }
            )
        }
    }
}
