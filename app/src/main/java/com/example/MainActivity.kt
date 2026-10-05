package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.network.MahakalRetrofitClient
import com.example.core.network.SessionManager
import com.example.core.security.SecureTokenStorage
import com.example.data.local.MahakalDatabase
import com.example.data.repository.MahakalRepository
import com.example.ui.screens.AdminDashboardScreen
import com.example.ui.screens.AgentDashboardScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.UserDashboardScreen
import com.example.ui.theme.MahakalTheme
import com.example.ui.viewmodel.MahakalViewModel
import kotlinx.coroutines.flow.collectLatest

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as? MahakalApplication
        val repository = app?.repository ?: run {
            val tokenStorage = SecureTokenStorage(applicationContext)
            val sessionManager = SessionManager(tokenStorage)
            val apiService = MahakalRetrofitClient.create(tokenStorage)
            val db = MahakalDatabase.getInstance(applicationContext)
            MahakalRepository(db, apiService, sessionManager, tokenStorage)
        }
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
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(Unit) {
        viewModel.snackbarMessage.collectLatest { msg ->
            snackbarHostState.showSnackbar(msg)
        }
    }

    if (currentUser == null) {
        LoginScreen(viewModel = viewModel)
    } else {
        val user = currentUser!!
        when (user.role) {
            "ADMIN" -> {
                com.example.ui.screens.AdminDashboardScreen(
                    admin = user,
                    viewModel = viewModel
                )
            }
            "AGENT" -> {
                com.example.ui.screens.AgentDashboardScreen(
                    agent = user,
                    viewModel = viewModel
                )
            }
            else -> {
                com.example.ui.screens.UserDashboardScreen(
                    user = user,
                    viewModel = viewModel
                )
            }
        }
    }
}
