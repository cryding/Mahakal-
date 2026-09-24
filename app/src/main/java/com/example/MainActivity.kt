package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.network.AuthState
import com.example.ui.admin.AdminAgentViewModel
import com.example.ui.admin.AdminDashboardScreen
import com.example.ui.security.SecurityViewModel
import com.example.ui.agent.AgentDashboardScreen
import com.example.ui.agent.AgentUserViewModel
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.LoginViewModel
import com.example.ui.game.GameViewModel
import com.example.ui.landing.UserLandingScreen
import com.example.ui.notification.NotificationViewModel
import com.example.ui.notification.NotificationsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.wallet.WalletViewModel

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as MahakalApplication
        val authRepository = app.authRepository
        val agentRepository = app.agentRepository
        val userRepository = app.userRepository
        val walletRepository = app.walletRepository
        val gameRepository = app.gameRepository
        val notificationRepository = app.notificationRepository
        val securityRepository = app.securityRepository

        setContent {
            MyApplicationTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val loginViewModel: LoginViewModel = viewModel(
                        factory = LoginViewModel.Factory(authRepository)
                    )
                    val adminAgentViewModel: AdminAgentViewModel = viewModel(
                        factory = AdminAgentViewModel.Factory(agentRepository)
                    )
                    val agentUserViewModel: AgentUserViewModel = viewModel(
                        factory = AgentUserViewModel.Factory(userRepository)
                    )
                    val walletViewModel: WalletViewModel = viewModel(
                        factory = WalletViewModel.Factory(walletRepository)
                    )
                    val gameViewModel: GameViewModel = viewModel(
                        factory = GameViewModel.Factory(gameRepository)
                    )
                    val notificationViewModel: NotificationViewModel = viewModel(
                        factory = NotificationViewModel.Factory(notificationRepository)
                    )
                    val securityViewModel: SecurityViewModel = viewModel(
                        factory = SecurityViewModel.Factory(securityRepository)
                    )
                    MahakalAppNavigation(
                        loginViewModel = loginViewModel,
                        adminAgentViewModel = adminAgentViewModel,
                        agentUserViewModel = agentUserViewModel,
                        walletViewModel = walletViewModel,
                        gameViewModel = gameViewModel,
                        notificationViewModel = notificationViewModel,
                        securityViewModel = securityViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun MahakalAppNavigation(
    loginViewModel: LoginViewModel,
    adminAgentViewModel: AdminAgentViewModel,
    agentUserViewModel: AgentUserViewModel,
    walletViewModel: WalletViewModel,
    gameViewModel: GameViewModel,
    notificationViewModel: NotificationViewModel,
    securityViewModel: SecurityViewModel? = null
) {
    val authState by loginViewModel.authState.collectAsState()
    var showNotifications by remember { mutableStateOf(false) }

    when (val state = authState) {
        is AuthState.Authenticated -> {
            LaunchedEffect(state.user.id) {
                notificationViewModel.refreshAll()
            }

            if (showNotifications) {
                BackHandler {
                    showNotifications = false
                }
                NotificationsScreen(
                    viewModel = notificationViewModel,
                    onBack = { showNotifications = false }
                )
            } else {
                when (state.user.role.uppercase()) {
                    "ADMIN" -> AdminDashboardScreen(
                        adminUser = state.user,
                        viewModel = adminAgentViewModel,
                        agentUserViewModel = agentUserViewModel,
                        walletViewModel = walletViewModel,
                        gameViewModel = gameViewModel,
                        notificationViewModel = notificationViewModel,
                        securityViewModel = securityViewModel,
                        onOpenNotifications = { showNotifications = true },
                        onLogout = { loginViewModel.logout() }
                    )
                    "AGENT" -> AgentDashboardScreen(
                        agentUser = state.user,
                        viewModel = agentUserViewModel,
                        walletViewModel = walletViewModel,
                        notificationViewModel = notificationViewModel,
                        onOpenNotifications = { showNotifications = true },
                        onLogout = { loginViewModel.logout() }
                    )
                    else -> UserLandingScreen(
                        user = state.user,
                        walletViewModel = walletViewModel,
                        gameViewModel = gameViewModel,
                        notificationViewModel = notificationViewModel,
                        onOpenNotifications = { showNotifications = true },
                        onLogout = { loginViewModel.logout() }
                    )
                }
            }
        }
        else -> {
            LoginScreen(viewModel = loginViewModel)
        }
    }
}
