package com.example.ui.notification

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MonetizationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.backend.model.NotificationDto
import com.example.backend.model.NotificationPreferenceDto
import com.example.backend.model.NotificationSeverity
import com.example.backend.model.NotificationStatus
import com.example.backend.model.NotificationType
import com.example.backend.model.UpdateNotificationPreferenceRequest
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsScreen(
    viewModel: NotificationViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val unreadCount by viewModel.unreadCount.collectAsState()
    val selectedNotification by viewModel.selectedNotification.collectAsState()
    val preferences by viewModel.preferences.collectAsState()
    val statusFilter by viewModel.statusFilter.collectAsState()
    val typeFilter by viewModel.typeFilter.collectAsState()
    val isRefreshing by viewModel.isRefreshing.collectAsState()

    var showPreferencesDialog by remember { mutableStateOf(false) }
    var latestRealtimeAlert by remember { mutableStateOf<NotificationDto?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    // Listen for realtime push alerts
    LaunchedEffect(Unit) {
        viewModel.inAppAlert.collect { alert ->
            latestRealtimeAlert = alert
            delay(5000)
            if (latestRealtimeAlert == alert) {
                latestRealtimeAlert = null
            }
        }
    }

    // Listen for operation feedback messages
    LaunchedEffect(Unit) {
        viewModel.operationMessage.collect { message ->
            snackbarHostState.showSnackbar(message)
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().testTag("notifications_screen"),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "Notifications",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        if (unreadCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.error
                            ) {
                                Text(
                                    text = "$unreadCount new",
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onError,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("notif_back_btn")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.loadNotifications() },
                        modifier = Modifier.testTag("notif_refresh_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh Notifications"
                        )
                    }
                    IconButton(
                        onClick = { viewModel.markAllAsRead() },
                        modifier = Modifier.testTag("notif_mark_all_read_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.DoneAll,
                            contentDescription = "Mark All Read"
                        )
                    }
                    IconButton(
                        onClick = {
                            viewModel.loadPreferences()
                            showPreferencesDialog = true
                        },
                        modifier = Modifier.testTag("notif_preferences_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Notification Preferences"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 700.dp)
                    .align(Alignment.TopCenter)
            ) {
                // Filters Header
                NotificationFiltersSection(
                    selectedStatus = statusFilter,
                    selectedType = typeFilter,
                    onStatusSelected = { viewModel.setStatusFilter(it) },
                    onTypeSelected = { viewModel.setTypeFilter(it) }
                )

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

                // Notification Content
                when (val state = uiState) {
                    is NotificationListUiState.Loading -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator()
                        }
                    }
                    is NotificationListUiState.Error -> {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(32.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    imageVector = Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    modifier = Modifier.size(48.dp),
                                    tint = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = state.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.error
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Button(onClick = { viewModel.loadNotifications() }) {
                                    Text("Retry")
                                }
                            }
                        }
                    }
                    is NotificationListUiState.Success -> {
                        if (state.notifications.isEmpty()) {
                            NotificationEmptyState(
                                statusFilter = statusFilter,
                                onResetFilters = {
                                    viewModel.setStatusFilter(null)
                                    viewModel.setTypeFilter(null)
                                }
                            )
                        } else {
                            LazyColumn(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .testTag("notifications_list"),
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                items(
                                    items = state.notifications,
                                    key = { it.notificationId }
                                ) { notification ->
                                    NotificationCard(
                                        notification = notification,
                                        onClick = { viewModel.selectNotification(notification) },
                                        onMarkAsRead = { viewModel.markAsRead(notification.notificationId) },
                                        onArchive = { viewModel.archiveNotification(notification.notificationId) }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Real-time In-App Pop Alert Banner
            AnimatedVisibility(
                visible = latestRealtimeAlert != null,
                enter = slideInVertically(initialOffsetY = { -it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { -it }) + fadeOut(),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            ) {
                latestRealtimeAlert?.let { alert ->
                    InAppRealtimeAlertBanner(
                        alert = alert,
                        onView = {
                            viewModel.selectNotification(alert)
                            latestRealtimeAlert = null
                        },
                        onDismiss = { latestRealtimeAlert = null }
                    )
                }
            }
        }
    }

    // Notification Details Dialog
    selectedNotification?.let { notif ->
        NotificationDetailDialog(
            notification = notif,
            onDismiss = { viewModel.selectNotification(null) },
            onArchive = {
                viewModel.archiveNotification(notif.notificationId)
                viewModel.selectNotification(null)
            }
        )
    }

    // Preferences Dialog
    if (showPreferencesDialog) {
        NotificationPreferencesDialog(
            preferences = preferences,
            onDismiss = { showPreferencesDialog = false },
            onSave = { updated ->
                viewModel.updatePreferences(updated)
                showPreferencesDialog = false
            }
        )
    }
}

@Composable
fun NotificationFiltersSection(
    selectedStatus: String?,
    selectedType: String?,
    onStatusSelected: (String?) -> Unit,
    onTypeSelected: (String?) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Status Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedStatus == null,
                    onClick = { onStatusSelected(null) },
                    label = { Text("All Status") },
                    colors = FilterChipDefaults.filterChipColors()
                )
            }
            item {
                FilterChip(
                    selected = selectedStatus == NotificationStatus.UNREAD,
                    onClick = { onStatusSelected(if (selectedStatus == NotificationStatus.UNREAD) null else NotificationStatus.UNREAD) },
                    label = { Text("Unread") }
                )
            }
            item {
                FilterChip(
                    selected = selectedStatus == NotificationStatus.READ,
                    onClick = { onStatusSelected(if (selectedStatus == NotificationStatus.READ) null else NotificationStatus.READ) },
                    label = { Text("Read") }
                )
            }
            item {
                FilterChip(
                    selected = selectedStatus == NotificationStatus.ARCHIVED,
                    onClick = { onStatusSelected(if (selectedStatus == NotificationStatus.ARCHIVED) null else NotificationStatus.ARCHIVED) },
                    label = { Text("Archived") }
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Type Row
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                FilterChip(
                    selected = selectedType == null,
                    onClick = { onTypeSelected(null) },
                    label = { Text("All Types") }
                )
            }
            item {
                FilterChip(
                    selected = selectedType == NotificationType.GAME_OPENED,
                    onClick = { onTypeSelected(if (selectedType == NotificationType.GAME_OPENED) null else NotificationType.GAME_OPENED) },
                    label = { Text("Games") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            item {
                FilterChip(
                    selected = selectedType == NotificationType.VIRTUAL_COIN_RECEIVED,
                    onClick = { onTypeSelected(if (selectedType == NotificationType.VIRTUAL_COIN_RECEIVED) null else NotificationType.VIRTUAL_COIN_RECEIVED) },
                    label = { Text("Virtual Coins") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.MonetizationOn,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            item {
                FilterChip(
                    selected = selectedType == NotificationType.PASSWORD_CHANGED,
                    onClick = { onTypeSelected(if (selectedType == NotificationType.PASSWORD_CHANGED) null else NotificationType.PASSWORD_CHANGED) },
                    label = { Text("Security") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
            item {
                FilterChip(
                    selected = selectedType == NotificationType.ADMIN_OPERATIONAL_ALERT,
                    onClick = { onTypeSelected(if (selectedType == NotificationType.ADMIN_OPERATIONAL_ALERT) null else NotificationType.ADMIN_OPERATIONAL_ALERT) },
                    label = { Text("Alerts") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                )
            }
        }
    }
}

@Composable
fun NotificationCard(
    notification: NotificationDto,
    onClick: () -> Unit,
    onMarkAsRead: () -> Unit,
    onArchive: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isUnread = notification.status == NotificationStatus.UNREAD
    val (icon, iconColor, containerBg) = getNotificationVisuals(notification.type, notification.severity, isUnread)

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .testTag("notif_item_${notification.notificationId}"),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isUnread) containerBg else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isUnread) 2.dp else 0.5.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Severity / Category Icon with Avatar Container
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = notification.type,
                    tint = iconColor,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Body content
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isUnread) FontWeight.Bold else FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )

                    if (isUnread) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = formatNotificationTime(notification.createdAt),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        if (isUnread) {
                            IconButton(
                                onClick = onMarkAsRead,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Mark Read",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        if (notification.status != NotificationStatus.ARCHIVED) {
                            IconButton(
                                onClick = onArchive,
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Archive,
                                    contentDescription = "Archive",
                                    tint = MaterialTheme.colorScheme.outline,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun NotificationDetailDialog(
    notification: NotificationDto,
    onDismiss: () -> Unit,
    onArchive: () -> Unit
) {
    val (icon, iconColor, _) = getNotificationVisuals(
        notification.type,
        notification.severity,
        notification.status == NotificationStatus.UNREAD
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(iconColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = iconColor,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = notification.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Severity: ${notification.severity} • ${notification.type}",
                        style = MaterialTheme.typography.labelSmall,
                        color = iconColor
                    )
                }
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = notification.body,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(12.dp))

                DetailItem(label = "Delivered", value = formatFullDateTime(notification.createdAt))
                if (notification.readAt != null) {
                    DetailItem(label = "Read At", value = formatFullDateTime(notification.readAt))
                }
                if (notification.referenceType != null && notification.referenceId != null) {
                    DetailItem(
                        label = "Reference",
                        value = "${notification.referenceType}: ${notification.referenceId}"
                    )
                }
                if (notification.correlationId != null) {
                    DetailItem(
                        label = "Tracking ID",
                        value = notification.correlationId
                    )
                }
            }
        },
        confirmButton = {
            Button(onClick = onDismiss) {
                Text("Close")
            }
        },
        dismissButton = {
            if (notification.status != NotificationStatus.ARCHIVED) {
                OutlinedButton(onClick = onArchive) {
                    Text("Archive")
                }
            }
        }
    )
}

@Composable
private fun DetailItem(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.outline
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun NotificationPreferencesDialog(
    preferences: NotificationPreferenceDto?,
    onDismiss: () -> Unit,
    onSave: (UpdateNotificationPreferenceRequest) -> Unit
) {
    var gameEnabled by remember(preferences) {
        mutableStateOf(preferences?.gameNotificationsEnabled ?: true)
    }
    var resultEnabled by remember(preferences) {
        mutableStateOf(preferences?.resultNotificationsEnabled ?: true)
    }
    var walletEnabled by remember(preferences) {
        mutableStateOf(preferences?.walletNotificationsEnabled ?: true)
    }
    var operationalEnabled by remember(preferences) {
        mutableStateOf(preferences?.operationalNotificationsEnabled ?: true)
    }
    var pushEnabled by remember(preferences) {
        mutableStateOf(preferences?.pushEnabled ?: true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "Notification Settings",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Manage which non-monetary system events generate notifications.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                PreferenceSwitchItem(
                    title = "Game Events",
                    subtitle = "New games, deadline reminders, and cancellations",
                    checked = gameEnabled,
                    onCheckedChange = { gameEnabled = it }
                )

                PreferenceSwitchItem(
                    title = "Results & Settlement",
                    subtitle = "Game outcomes and virtual coin reward updates",
                    checked = resultEnabled,
                    onCheckedChange = { resultEnabled = it }
                )

                PreferenceSwitchItem(
                    title = "Virtual Coin Ledger",
                    subtitle = "Transfers received and coin deductions",
                    checked = walletEnabled,
                    onCheckedChange = { walletEnabled = it }
                )

                // Mandatory Security Setting - Cannot be disabled
                PreferenceSwitchItem(
                    title = "Security & Auth Alerts",
                    subtitle = "Mandatory: Password changes and account status (Always ON)",
                    checked = true,
                    enabled = false,
                    onCheckedChange = {}
                )

                PreferenceSwitchItem(
                    title = "Operational Alerts",
                    subtitle = "System notices and critical event failures",
                    checked = operationalEnabled,
                    onCheckedChange = { operationalEnabled = it }
                )

                PreferenceSwitchItem(
                    title = "In-App Push Updates",
                    subtitle = "Real-time foreground popups for incoming events",
                    checked = pushEnabled,
                    onCheckedChange = { pushEnabled = it }
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        UpdateNotificationPreferenceRequest(
                            gameNotificationsEnabled = gameEnabled,
                            resultNotificationsEnabled = resultEnabled,
                            walletNotificationsEnabled = walletEnabled,
                            operationalNotificationsEnabled = operationalEnabled,
                            pushEnabled = pushEnabled
                        )
                    )
                },
                modifier = Modifier.testTag("save_notif_prefs_btn")
            ) {
                Text("Save Changes")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun PreferenceSwitchItem(
    title: String,
    subtitle: String,
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.outline
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.outline
            )
        }
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            enabled = enabled
        )
    }
}

@Composable
fun InAppRealtimeAlertBanner(
    alert: NotificationDto,
    onView: () -> Unit,
    onDismiss: () -> Unit
) {
    val (icon, iconColor, _) = getNotificationVisuals(alert.type, alert.severity, isUnread = true)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("realtime_alert_banner"),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        elevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = iconColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = alert.title,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = alert.body,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = onView,
                modifier = Modifier.height(36.dp),
                contentPadding = PaddingValues(horizontal = 12.dp)
            ) {
                Text("View", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
fun NotificationEmptyState(
    statusFilter: String?,
    onResetFilters: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Notifications,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.outlineVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = if (statusFilter != null) "No ${statusFilter.lowercase()} notifications" else "You're all caught up!",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "New game updates, virtual coin movements, and security alerts will appear here.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.outline,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (statusFilter != null) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedButton(onClick = onResetFilters) {
                    Text("Clear Filters")
                }
            }
        }
    }
}

private data class NotificationVisuals(
    val icon: ImageVector,
    val iconColor: Color,
    val containerBg: Color
)

@Composable
private fun getNotificationVisuals(
    type: String,
    severity: String,
    isUnread: Boolean
): NotificationVisuals {
    val icon = when (type) {
        NotificationType.GAME_OPENED,
        NotificationType.GAME_CLOSING,
        NotificationType.GAME_CLOSED,
        NotificationType.RESULT_FINALIZED,
        NotificationType.ENTRY_CONFIRMED,
        NotificationType.ENTRY_REFUNDED -> Icons.Default.SportsEsports

        NotificationType.VIRTUAL_COIN_RECEIVED,
        NotificationType.VIRTUAL_COIN_DEDUCTED -> Icons.Default.MonetizationOn

        NotificationType.PASSWORD_CHANGED,
        NotificationType.ACCOUNT_SUSPENDED,
        NotificationType.ACCOUNT_ACTIVATED,
        NotificationType.SECURITY_EVENT -> Icons.Default.Security

        NotificationType.ADMIN_OPERATIONAL_ALERT -> Icons.Default.Warning
        else -> Icons.Default.Notifications
    }

    val iconColor = when (severity) {
        NotificationSeverity.CRITICAL -> MaterialTheme.colorScheme.error
        NotificationSeverity.WARNING -> Color(0xFFE65100) // Deep Orange
        else -> MaterialTheme.colorScheme.primary
    }

    val containerBg = when {
        severity == NotificationSeverity.CRITICAL -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)
        severity == NotificationSeverity.WARNING -> Color(0xFFFFF3E0)
        isUnread -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
        else -> MaterialTheme.colorScheme.surface
    }

    return NotificationVisuals(icon, iconColor, containerBg)
}

private fun formatNotificationTime(timestamp: Long): String {
    val now = System.currentTimeMillis()
    val diff = now - timestamp
    return when {
        diff < 60_000L -> "Just now"
        diff < 3600_000L -> "${diff / 60_000L}m ago"
        diff < 86400_000L -> "${diff / 3600_000L}h ago"
        else -> SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(timestamp))
    }
}

private fun formatFullDateTime(timestamp: Long): String {
    return SimpleDateFormat("dd MMM yyyy, hh:mm:ss a", Locale.getDefault()).format(Date(timestamp))
}
