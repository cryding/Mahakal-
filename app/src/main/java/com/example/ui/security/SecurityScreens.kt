package com.example.ui.security

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.backend.model.SecurityDashboardSummaryDto
import com.example.backend.model.SecurityEventDto
import com.example.backend.model.SessionInfoDto
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val BgDark = Color(0xFF0A0E17)
private val CardBg = Color(0xFF141C2B)
private val CardBorder = Color(0xFF233044)
private val AccentGold = Color(0xFFFFB300)
private val SecurityGreen = Color(0xFF4CAF50)
private val SecurityRed = Color(0xFFEF5350)
private val TextMuted = Color(0xFF8A99AD)

@Composable
fun AdminSecurityScreen(
    securityViewModel: SecurityViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by securityViewModel.uiState.collectAsState()
    var selectedSection by remember { mutableStateOf("METRICS") } // METRICS, SESSIONS, EVENTS

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(BgDark)
            .padding(16.dp)
            .testTag("admin_security_screen")
    ) {
        // Top Bar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = "Security",
                        tint = AccentGold,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Security & Integrity Center",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
                Text(
                    text = "Real-time threat monitoring, abuse defense & session integrity",
                    fontSize = 11.sp,
                    color = TextMuted
                )
            }

            IconButton(
                onClick = {
                    securityViewModel.loadDashboardSummary()
                    securityViewModel.loadActiveSessions()
                    securityViewModel.loadHealthCheck()
                },
                modifier = Modifier.testTag("sec_refresh_button")
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh Security Data",
                    tint = AccentGold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // System Health Strip
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = Color(0xFF101926),
            border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Health Status",
                        tint = if (uiState.healthCheck?.status == "OK") SecurityGreen else AccentGold,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "System: ${uiState.healthCheck?.status ?: "OK"}",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = "Posture: ${uiState.dashboardSummary?.systemStatus ?: "HEALTHY"}",
                        fontSize = 11.sp,
                        color = if (uiState.dashboardSummary?.systemStatus == "ELEVATED_ALERT") SecurityRed else SecurityGreen
                    )
                }

                Text(
                    text = "Mode: Non-Monetary Engine",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    color = AccentGold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Section Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            FilterChip(
                selected = selectedSection == "METRICS",
                onClick = { selectedSection = "METRICS" },
                label = { Text("THREAT METRICS", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentGold,
                    selectedLabelColor = Color.Black
                ),
                modifier = Modifier.testTag("sec_tab_metrics")
            )
            FilterChip(
                selected = selectedSection == "SESSIONS",
                onClick = {
                    selectedSection = "SESSIONS"
                    securityViewModel.loadActiveSessions()
                },
                label = { Text("ACTIVE SESSIONS (${uiState.activeSessions.size})", fontSize = 11.sp) },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = AccentGold,
                    selectedLabelColor = Color.Black
                ),
                modifier = Modifier.testTag("sec_tab_sessions")
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (uiState.isLoading && uiState.dashboardSummary == null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = AccentGold)
            }
        } else if (selectedSection == "METRICS") {
            val summary = uiState.dashboardSummary
            if (summary != null) {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecMetricCard(
                        title = "Failed Logins",
                        value = "${summary.failedLoginCount}",
                        subtitle = "Brute-force window",
                        isWarning = summary.failedLoginCount > 5,
                        modifier = Modifier.weight(1f)
                    )
                    SecMetricCard(
                        title = "Active Sessions",
                        value = "${summary.activeSessionsCount}",
                        subtitle = "Revoked: ${summary.revokedSessionsCount}",
                        isWarning = false,
                        modifier = Modifier.weight(1f)
                    )
                    SecMetricCard(
                        title = "Locked Accounts",
                        value = "${summary.lockedAccountsCount}",
                        subtitle = "Rate-limited users",
                        isWarning = summary.lockedAccountsCount > 0,
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SecMetricCard(
                        title = "Privilege Violations",
                        value = "${summary.authorizationFailuresCount}",
                        subtitle = "RBAC 403 blocks",
                        isWarning = summary.authorizationFailuresCount > 0,
                        modifier = Modifier.weight(1f)
                    )
                    SecMetricCard(
                        title = "IDOR Defenses",
                        value = "${summary.idorAttemptsCount}",
                        subtitle = "Cross-account hits",
                        isWarning = summary.idorAttemptsCount > 0,
                        modifier = Modifier.weight(1f)
                    )
                    SecMetricCard(
                        title = "Token Misuse",
                        value = "${summary.tokenReuseCount}",
                        subtitle = "Replay protections",
                        isWarning = summary.tokenReuseCount > 0,
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Security Events Stream",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            val recentEvents = summary?.recentSecurityEvents ?: emptyList()
            if (recentEvents.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No anomalous security events detected.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(recentEvents) { event ->
                        SecEventRow(event = event)
                    }
                }
            }
        } else {
            // SESSIONS View
            Text(
                text = "Device Sessions & Token Revocation",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.activeSessions.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = CardBg,
                    border = androidx.compose.foundation.BorderStroke(1.dp, CardBorder),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "No active sessions found.",
                            fontSize = 12.sp,
                            color = TextMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.activeSessions) { session ->
                        SecSessionRow(
                            session = session,
                            onRevoke = { securityViewModel.revokeSession(session.sessionId) },
                            onRevokeAll = { securityViewModel.revokeAllAccountSessions(session.accountId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun SecMetricCard(
    title: String,
    value: String,
    subtitle: String,
    isWarning: Boolean,
    modifier: Modifier = Modifier
) {
    val cardBorder = if (isWarning) SecurityRed else CardBorder
    val valueColor = if (isWarning) SecurityRed else AccentGold

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = CardBg),
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(10.dp)
        ) {
            Text(
                text = title,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = TextMuted
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = value,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = valueColor
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = subtitle,
                fontSize = 9.sp,
                color = Color(0xFF758292)
            )
        }
    }
}

@Composable
fun SecEventRow(event: SecurityEventDto) {
    val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    val isAlert = event.eventType.contains("FAILED") ||
            event.eventType.contains("DENIED") ||
            event.eventType.contains("LOCKED") ||
            event.eventType.contains("IDOR") ||
            event.eventType.contains("REUSE")
    val sevColor = if (isAlert) SecurityRed else SecurityGreen

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF131A26),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222C3C)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = sevColor.copy(alpha = 0.2f),
                        border = androidx.compose.foundation.BorderStroke(1.dp, sevColor)
                    ) {
                        Text(
                            text = if (isAlert) "ALERT" else "AUDIT",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = sevColor,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = event.eventType,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                }

                Text(
                    text = timeFormat.format(Date(event.timestamp)),
                    fontSize = 10.sp,
                    color = Color(0xFF8D99AE)
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "Actor: ${event.actorId.take(12)} (${event.actorRole})",
                    fontSize = 10.sp,
                    color = Color(0xFF94A3B8)
                )
                Text(
                    text = "Req: ${event.requestId.take(8)}",
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color(0xFF64748B)
                )
            }
        }
    }
}

@Composable
fun SecSessionRow(
    session: SessionInfoDto,
    onRevoke: () -> Unit,
    onRevokeAll: () -> Unit
) {
    val timeFormat = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

    Surface(
        shape = RoundedCornerShape(8.dp),
        color = Color(0xFF131A26),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF222C3C)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Account: ${session.accountId.take(16)}...",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color.White
                    )
                    Text(
                        text = "Role: ${session.role} | IP: ${session.ipAddress ?: "Direct"}",
                        fontSize = 10.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                Text(
                    text = if (session.isRevoked) "REVOKED" else "ACTIVE",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (session.isRevoked) SecurityRed else SecurityGreen
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = "Last Active: ${timeFormat.format(Date(session.lastActivityAt))}",
                fontSize = 10.sp,
                color = Color(0xFF64748B)
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!session.isRevoked) {
                    OutlinedButton(
                        onClick = onRevoke,
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = SecurityRed),
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Text("Revoke Session", fontSize = 10.sp)
                    }
                    Button(
                        onClick = onRevokeAll,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
                    ) {
                        Text("Revoke All", fontSize = 10.sp, color = Color.White)
                    }
                }
            }
        }
    }
}
