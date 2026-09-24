package com.example.backend.service

import com.example.backend.audit.AuditActions
import com.example.backend.audit.AuditService
import com.example.backend.database.MahakalServerDatabase
import com.example.backend.database.entity.DeviceSessionEntity
import com.example.backend.database.entity.GameEntity
import com.example.backend.database.entity.GameEntryEntity
import com.example.backend.database.entity.NotificationEntity
import com.example.backend.database.entity.NotificationPreferenceEntity
import com.example.backend.database.entity.OutboxEventEntity
import com.example.backend.database.entity.WalletTransactionEntity
import com.example.backend.model.NotificationDto
import com.example.backend.model.NotificationListResponse
import com.example.backend.model.NotificationPreferenceDto
import com.example.backend.model.NotificationReferenceType
import com.example.backend.model.NotificationSeverity
import com.example.backend.model.NotificationStatus
import com.example.backend.model.NotificationType
import com.example.backend.model.RegisterDeviceRequest
import com.example.backend.model.ServerResponse
import com.example.backend.model.UnreadCountResponse
import com.example.backend.model.UpdateNotificationPreferenceRequest
import com.example.backend.rbac.AccountRole
import com.example.backend.rbac.SecurityContext
import com.example.backend.security.RateLimiter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.util.UUID

/**
 * Server-authoritative, persistent Notification and Event-Delivery Service.
 * Ensures reliable outbox event processing, strict recipient ownership,
 * idempotency deduplication, and non-monetary transactional notifications.
 */
class NotificationService(
    private val database: MahakalServerDatabase,
    private val auditService: AuditService,
    private val rateLimiter: RateLimiter
) {
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val notificationDao = database.notificationDao()
    private val preferenceDao = database.notificationPreferenceDao()
    private val outboxDao = database.outboxDao()
    private val deviceSessionDao = database.deviceSessionDao()

    // Real-time broadcast stream for connected clients
    private val _realtimeNotificationFlow = MutableSharedFlow<NotificationDto>(extraBufferCapacity = 64)
    val realtimeNotificationFlow: SharedFlow<NotificationDto> = _realtimeNotificationFlow.asSharedFlow()

    // ==========================================
    // DOMAIN EVENT PRODUCERS (Server-Authoritative)
    // ==========================================

    /**
     * Emits a game opened notification to eligible players.
     */
    suspend fun onGameOpened(game: GameEntity, actorId: String) {
        val payload = JSONObject().apply {
            put("gameId", game.gameId)
            put("title", game.title)
            put("gameType", game.gameType)
            put("minCoins", game.minCoins)
            put("maxCoins", game.maxCoins)
            put("entryDeadline", game.entryDeadline)
        }.toString()

        recordOutboxAndDeliver(
            eventType = NotificationType.GAME_OPENED,
            aggregateType = NotificationReferenceType.GAME,
            aggregateId = game.gameId,
            payload = payload,
            correlationId = "game_open_${game.gameId}"
        ) {
            // Deliver to all confirmed USER accounts
            val notifs = mutableListOf<NotificationEntity>()
            val users = database.accountDao().findByRole(AccountRole.USER.name)
            for (user in users) {
                val n = createNotificationIfPermitted(
                    recipientId = user.id,
                    recipientRole = user.role,
                    type = NotificationType.GAME_OPENED,
                    title = "Game Open: ${game.title}",
                    body = "Predictions are now open for ${game.title}. Enter before the deadline.",
                    severity = NotificationSeverity.INFO,
                    referenceType = NotificationReferenceType.GAME,
                    referenceId = game.gameId,
                    correlationId = "game_open_${game.gameId}_${user.id}"
                )
                if (n != null) notifs.add(n)
            }
            notifs
        }
    }

    /**
     * Emits a game closing alert when a game is nearing entry deadline.
     */
    suspend fun onGameClosing(game: GameEntity) {
        val payload = JSONObject().apply {
            put("gameId", game.gameId)
            put("title", game.title)
            put("entryDeadline", game.entryDeadline)
        }.toString()

        recordOutboxAndDeliver(
            eventType = NotificationType.GAME_CLOSING,
            aggregateType = NotificationReferenceType.GAME,
            aggregateId = game.gameId,
            correlationId = "game_closing_${game.gameId}"
        ) {
            val notifs = mutableListOf<NotificationEntity>()
            val users = database.accountDao().findByRole(AccountRole.USER.name)
            for (user in users) {
                val n = createNotificationIfPermitted(
                    recipientId = user.id,
                    recipientRole = user.role,
                    type = NotificationType.GAME_CLOSING,
                    title = "Deadline Soon: ${game.title}",
                    body = "Entry deadline for ${game.title} is closing shortly.",
                    severity = NotificationSeverity.WARNING,
                    referenceType = NotificationReferenceType.GAME,
                    referenceId = game.gameId,
                    correlationId = "game_closing_${game.gameId}_${user.id}"
                )
                if (n != null) notifs.add(n)
            }
            notifs
        }
    }

    /**
     * Emits entry confirmation notification to the player who entered.
     */
    suspend fun onGameEntryConfirmed(entry: GameEntryEntity, gameTitle: String) {
        val payload = JSONObject().apply {
            put("entryId", entry.entryId)
            put("gameId", entry.gameId)
            put("userId", entry.userId)
            put("virtualCoins", entry.virtualCoinAmount)
        }.toString()

        recordOutboxAndDeliver(
            eventType = NotificationType.ENTRY_CONFIRMED,
            aggregateType = NotificationReferenceType.ENTRY,
            aggregateId = entry.entryId,
            payload = payload,
            correlationId = "entry_confirmed_${entry.entryId}"
        ) {
            val notif = createNotificationIfPermitted(
                recipientId = entry.userId,
                recipientRole = AccountRole.USER.name,
                type = NotificationType.ENTRY_CONFIRMED,
                title = "Entry Confirmed: $gameTitle",
                body = "Your entry of ${entry.virtualCoinAmount} virtual coins has been placed successfully.",
                severity = NotificationSeverity.INFO,
                referenceType = NotificationReferenceType.ENTRY,
                referenceId = entry.entryId,
                correlationId = "entry_confirmed_${entry.entryId}"
            )
            listOfNotNull(notif)
        }
    }

    /**
     * Emits game results finalized notification to all players who participated.
     */
    suspend fun onGameResultFinalized(
        game: GameEntity,
        winningOptionName: String,
        entries: List<GameEntryEntity>
    ) {
        val payload = JSONObject().apply {
            put("gameId", game.gameId)
            put("winningOption", winningOptionName)
            put("totalParticipants", entries.size)
        }.toString()

        recordOutboxAndDeliver(
            eventType = NotificationType.RESULT_FINALIZED,
            aggregateType = NotificationReferenceType.GAME,
            aggregateId = game.gameId,
            payload = payload,
            correlationId = "result_finalized_${game.gameId}"
        ) {
            val notifs = mutableListOf<NotificationEntity>()
            for (entry in entries) {
                val isWinner = entry.rewardAmount != null && entry.rewardAmount > 0
                val body = if (isWinner) {
                    "Result finalized for ${game.title}. Congratulations! You won ${entry.rewardAmount} virtual coins on '$winningOptionName'."
                } else {
                    "Result finalized for ${game.title}. Winning option was '$winningOptionName'."
                }
                val severity = if (isWinner) NotificationSeverity.INFO else NotificationSeverity.INFO

                val n = createNotificationIfPermitted(
                    recipientId = entry.userId,
                    recipientRole = AccountRole.USER.name,
                    type = NotificationType.RESULT_FINALIZED,
                    title = if (isWinner) "Prediction Won: ${game.title}" else "Result Declared: ${game.title}",
                    body = body,
                    severity = severity,
                    referenceType = NotificationReferenceType.GAME,
                    referenceId = game.gameId,
                    correlationId = "result_finalized_${game.gameId}_${entry.entryId}"
                )
                if (n != null) notifs.add(n)
            }
            notifs
        }
    }

    /**
     * Emits notifications when a game is cancelled and entries are refunded.
     */
    suspend fun onGameCancelled(game: GameEntity, refundedEntries: List<GameEntryEntity>) {
        val payload = JSONObject().apply {
            put("gameId", game.gameId)
            put("refundedCount", refundedEntries.size)
        }.toString()

        recordOutboxAndDeliver(
            eventType = NotificationType.ENTRY_REFUNDED,
            aggregateType = NotificationReferenceType.GAME,
            aggregateId = game.gameId,
            payload = payload,
            correlationId = "game_cancelled_${game.gameId}"
        ) {
            val notifs = mutableListOf<NotificationEntity>()
            for (entry in refundedEntries) {
                val n = createNotificationIfPermitted(
                    recipientId = entry.userId,
                    recipientRole = AccountRole.USER.name,
                    type = NotificationType.ENTRY_REFUNDED,
                    title = "Game Cancelled: ${game.title}",
                    body = "${entry.virtualCoinAmount} virtual coins have been fully refunded to your wallet.",
                    severity = NotificationSeverity.WARNING,
                    referenceType = NotificationReferenceType.GAME,
                    referenceId = game.gameId,
                    correlationId = "game_cancelled_refund_${entry.entryId}"
                )
                if (n != null) notifs.add(n)
            }
            notifs
        }
    }

    /**
     * Emits virtual coin transaction notifications for sender and receiver.
     */
    suspend fun onWalletTransaction(
        tx: WalletTransactionEntity,
        sourceOwnerId: String?,
        destOwnerId: String?
    ) {
        val notifications = mutableListOf<NotificationEntity>()

        // Recipient received virtual coins
        if (destOwnerId != null && tx.amount > 0) {
            val notif = createNotificationIfPermitted(
                recipientId = destOwnerId,
                recipientRole = tx.actorRole,
                type = NotificationType.VIRTUAL_COIN_RECEIVED,
                title = "Virtual Coins Received",
                body = "You received ${tx.amount} virtual coins. Reason: ${tx.reason}.",
                severity = NotificationSeverity.INFO,
                referenceType = NotificationReferenceType.WALLET_TRANSACTION,
                referenceId = tx.transactionId,
                correlationId = "tx_rcv_${tx.transactionId}_$destOwnerId"
            )
            if (notif != null) notifications.add(notif)
        }

        // Sender deducted virtual coins
        if (sourceOwnerId != null && tx.amount > 0 && tx.transactionType != "GAME_ENTRY_DEDUCTION") {
            val notif = createNotificationIfPermitted(
                recipientId = sourceOwnerId,
                recipientRole = tx.actorRole,
                type = NotificationType.VIRTUAL_COIN_DEDUCTED,
                title = "Virtual Coins Transferred",
                body = "${tx.amount} virtual coins transferred. Reason: ${tx.reason}.",
                severity = NotificationSeverity.INFO,
                referenceType = NotificationReferenceType.WALLET_TRANSACTION,
                referenceId = tx.transactionId,
                correlationId = "tx_ded_${tx.transactionId}_$sourceOwnerId"
            )
            if (notif != null) notifications.add(notif)
        }

        if (notifications.isNotEmpty()) {
            recordOutboxAndDeliver(
                eventType = NotificationType.VIRTUAL_COIN_RECEIVED,
                aggregateType = NotificationReferenceType.WALLET_TRANSACTION,
                aggregateId = tx.transactionId,
                correlationId = "tx_${tx.transactionId}"
            ) {
                notifications
            }
        }
    }

    /**
     * Security Event: Password Changed (Mandatory delivery).
     */
    suspend fun onPasswordChanged(userId: String) {
        val user = database.accountDao().findById(userId) ?: return
        recordOutboxAndDeliver(
            eventType = NotificationType.PASSWORD_CHANGED,
            aggregateType = NotificationReferenceType.ACCOUNT,
            aggregateId = userId,
            correlationId = "pwd_chg_${userId}_${System.currentTimeMillis()}"
        ) {
            val notif = NotificationEntity(
                notificationId = UUID.randomUUID().toString(),
                recipientId = userId,
                recipientRole = user.role,
                type = NotificationType.PASSWORD_CHANGED,
                title = "Security Alert: Password Changed",
                body = "Your account password was successfully updated. If you did not make this change, contact support immediately.",
                severity = NotificationSeverity.CRITICAL,
                referenceType = NotificationReferenceType.ACCOUNT,
                referenceId = userId,
                status = NotificationStatus.UNREAD,
                createdAt = System.currentTimeMillis()
            )
            listOf(notif)
        }
    }

    /**
     * Security Event: Account Status Changed (Suspended or Activated).
     */
    suspend fun onAccountStatusChanged(userId: String, oldStatus: String, newStatus: String, actorId: String) {
        val user = database.accountDao().findById(userId) ?: return
        val isSuspended = newStatus == "SUSPENDED"
        val notifType = if (isSuspended) NotificationType.ACCOUNT_SUSPENDED else NotificationType.ACCOUNT_ACTIVATED
        val severity = if (isSuspended) NotificationSeverity.CRITICAL else NotificationSeverity.INFO
        val title = if (isSuspended) "Account Suspended" else "Account Activated"
        val body = if (isSuspended) {
            "Your account has been temporarily suspended by administration. Access to gameplay and wallet is restricted."
        } else {
            "Your account has been reactivated. You may now participate in games and view your balance."
        }

        recordOutboxAndDeliver(
            eventType = notifType,
            aggregateType = NotificationReferenceType.ACCOUNT,
            aggregateId = userId,
            correlationId = "status_change_${userId}_${System.currentTimeMillis()}"
        ) {
            val notif = NotificationEntity(
                notificationId = UUID.randomUUID().toString(),
                recipientId = userId,
                recipientRole = user.role,
                type = notifType,
                title = title,
                body = body,
                severity = severity,
                referenceType = NotificationReferenceType.ACCOUNT,
                referenceId = userId,
                status = NotificationStatus.UNREAD,
                createdAt = System.currentTimeMillis()
            )
            listOf(notif)
        }
    }

    /**
     * Admin Operational Alert (CRITICAL or WARNING).
     */
    suspend fun onAdminOperationalAlert(
        title: String,
        body: String,
        severity: String,
        referenceId: String? = null,
        correlationId: String? = null
    ) {
        val effectiveCorrelation = correlationId ?: "op_alert_${System.currentTimeMillis()}"
        recordOutboxAndDeliver(
            eventType = NotificationType.ADMIN_OPERATIONAL_ALERT,
            aggregateType = NotificationReferenceType.SYSTEM,
            aggregateId = referenceId ?: "SYSTEM",
            correlationId = effectiveCorrelation
        ) {
            val notifs = mutableListOf<NotificationEntity>()
            val admins = database.accountDao().findByRole(AccountRole.ADMIN.name)
            for (admin in admins) {
                val n = createNotificationIfPermitted(
                    recipientId = admin.id,
                    recipientRole = admin.role,
                    type = NotificationType.ADMIN_OPERATIONAL_ALERT,
                    title = title,
                    body = body,
                    severity = severity,
                    referenceType = NotificationReferenceType.SYSTEM,
                    referenceId = referenceId,
                    correlationId = "admin_alert_${admin.id}_$effectiveCorrelation"
                )
                if (n != null) notifs.add(n)
            }
            notifs
        }
    }

    // ==========================================
    // NOTIFICATION CREATION & PREFERENCE CHECK
    // ==========================================

    private suspend fun createNotificationIfPermitted(
        recipientId: String,
        recipientRole: String,
        type: String,
        title: String,
        body: String,
        severity: String,
        referenceType: String?,
        referenceId: String?,
        correlationId: String?
    ): NotificationEntity? {
        // 1. Check idempotency: If correlation ID exists for this recipient + type, skip duplicate
        if (correlationId != null) {
            val existing = notificationDao.countByCorrelationAndRecipient(correlationId, recipientId, type)
            if (existing > 0) return null
        }

        // 2. Check user notification preferences
        val prefs = preferenceDao.getPreferencesForUser(recipientId)
        if (prefs != null) {
            when (type) {
                NotificationType.GAME_OPENED,
                NotificationType.GAME_CLOSING,
                NotificationType.GAME_CLOSED,
                NotificationType.ENTRY_CONFIRMED,
                NotificationType.ENTRY_REFUNDED -> {
                    if (!prefs.gameNotificationsEnabled) return null
                }
                NotificationType.RESULT_FINALIZED -> {
                    if (!prefs.resultNotificationsEnabled) return null
                }
                NotificationType.VIRTUAL_COIN_RECEIVED,
                NotificationType.VIRTUAL_COIN_DEDUCTED -> {
                    if (!prefs.walletNotificationsEnabled) return null
                }
                NotificationType.ADMIN_OPERATIONAL_ALERT -> {
                    if (!prefs.operationalNotificationsEnabled) return null
                }
                // Security notifications are mandatory and CANNOT be turned off
                NotificationType.PASSWORD_CHANGED,
                NotificationType.ACCOUNT_SUSPENDED,
                NotificationType.ACCOUNT_ACTIVATED,
                NotificationType.SECURITY_EVENT -> {
                    // Mandatory - always delivered
                }
            }
        }

        val now = System.currentTimeMillis()
        return NotificationEntity(
            notificationId = UUID.randomUUID().toString(),
            recipientId = recipientId,
            recipientRole = recipientRole,
            type = type,
            title = title,
            body = body,
            severity = severity,
            referenceType = referenceType,
            referenceId = referenceId,
            status = NotificationStatus.UNREAD,
            createdAt = now,
            readAt = null,
            expiresAt = now + (30L * 24 * 60 * 60 * 1000), // 30-day retention
            correlationId = correlationId
        )
    }

    private suspend fun recordOutboxAndDeliver(
        eventType: String,
        aggregateType: String,
        aggregateId: String,
        payload: String = "{}",
        correlationId: String? = null,
        notificationBuilder: suspend () -> List<NotificationEntity>
    ) {
        val now = System.currentTimeMillis()
        val eventId = UUID.randomUUID().toString()

        // 1. Insert Outbox record
        val outbox = OutboxEventEntity(
            eventId = eventId,
            eventType = eventType,
            aggregateType = aggregateType,
            aggregateId = aggregateId,
            payload = payload,
            status = "PENDING",
            availableAt = now,
            correlationId = correlationId,
            createdAt = now
        )
        outboxDao.insertEvent(outbox)

        // 2. Deliver notifications safely within database
        try {
            val notifications = notificationBuilder()
            if (notifications.isNotEmpty()) {
                notificationDao.insertNotifications(notifications)
                notifications.forEach { notif ->
                    _realtimeNotificationFlow.tryEmit(notif.toDto())
                }
            }
            outboxDao.updateStatus(eventId, "COMPLETED", System.currentTimeMillis())
        } catch (e: Exception) {
            outboxDao.updateStatus(eventId, "FAILED", null, e.message)
        }
    }

    // ==========================================
    // CLIENT API OPERATIONS (Strict Recipient Scoped)
    // ==========================================

    suspend fun getNotifications(
        context: SecurityContext,
        page: Int = 1,
        limit: Int = 20,
        status: String? = null,
        type: String? = null,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<NotificationListResponse> {
        val safePage = if (page < 1) 1 else page
        val safeLimit = limit.coerceIn(1, 50)
        val offset = (safePage - 1) * safeLimit

        val entities = when {
            status != null -> notificationDao.getNotificationsForRecipientByStatus(context.accountId, status, safeLimit, offset)
            type != null -> notificationDao.getNotificationsForRecipientByType(context.accountId, type, safeLimit, offset)
            else -> notificationDao.getNotificationsForRecipient(context.accountId, safeLimit, offset)
        }

        val totalCount = notificationDao.countNotificationsForRecipient(context.accountId)
        val unreadCount = notificationDao.countUnreadForRecipient(context.accountId)

        return ServerResponse(
            success = true,
            statusCode = 200,
            data = NotificationListResponse(
                notifications = entities.map { it.toDto() },
                unreadCount = unreadCount,
                totalCount = totalCount,
                page = safePage,
                pageSize = safeLimit
            ),
            requestId = requestId
        )
    }

    suspend fun getNotificationDetails(
        context: SecurityContext,
        notificationId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<NotificationDto> {
        val notif = notificationDao.getNotificationById(notificationId)
            ?: return ServerResponse(false, 404, null, "NOT_FOUND", "Notification not found.", requestId)

        // Strict Recipient Ownership enforcement (prevents IDOR)
        if (notif.recipientId != context.accountId) {
            auditService.logEvent(
                actorId = context.accountId,
                actorRole = context.role.name,
                action = AuditActions.PERMISSION_DENIED,
                targetId = notificationId,
                targetType = "NOTIFICATION",
                requestId = requestId,
                metadataJson = "{\"attemptedRecipient\":\"${notif.recipientId}\"}"
            )
            return ServerResponse(false, 403, null, "FORBIDDEN", "You are not authorized to view this notification.", requestId)
        }

        return ServerResponse(true, 200, notif.toDto(), requestId = requestId)
    }

    suspend fun markAsRead(
        context: SecurityContext,
        notificationId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Boolean> {
        val notif = notificationDao.getNotificationById(notificationId)
            ?: return ServerResponse(false, 404, false, "NOT_FOUND", "Notification not found.", requestId)

        // Strict Recipient Ownership
        if (notif.recipientId != context.accountId) {
            return ServerResponse(false, 403, false, "FORBIDDEN", "Unauthorized action.", requestId)
        }

        notificationDao.markAsRead(notificationId, context.accountId, System.currentTimeMillis())
        return ServerResponse(true, 200, true, null, "Notification marked as read.", requestId)
    }

    suspend fun markAllAsRead(
        context: SecurityContext,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Int> {
        val updatedCount = notificationDao.markAllAsRead(context.accountId, System.currentTimeMillis())
        return ServerResponse(true, 200, updatedCount, null, "All notifications marked as read.", requestId)
    }

    suspend fun archiveNotification(
        context: SecurityContext,
        notificationId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Boolean> {
        val notif = notificationDao.getNotificationById(notificationId)
            ?: return ServerResponse(false, 404, false, "NOT_FOUND", "Notification not found.", requestId)

        if (notif.recipientId != context.accountId) {
            return ServerResponse(false, 403, false, "FORBIDDEN", "Unauthorized action.", requestId)
        }

        notificationDao.archiveNotification(notificationId, context.accountId)
        return ServerResponse(true, 200, true, null, "Notification archived.", requestId)
    }

    suspend fun getUnreadCount(
        context: SecurityContext,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<UnreadCountResponse> {
        val count = notificationDao.countUnreadForRecipient(context.accountId)
        return ServerResponse(true, 200, UnreadCountResponse(count, System.currentTimeMillis()), requestId = requestId)
    }

    // ==========================================
    // PREFERENCES MANAGEMENT
    // ==========================================

    suspend fun getPreferences(
        context: SecurityContext,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<NotificationPreferenceDto> {
        val entity = preferenceDao.getPreferencesForUser(context.accountId)
        val dto = if (entity != null) {
            NotificationPreferenceDto(
                userId = entity.userId,
                gameNotificationsEnabled = entity.gameNotificationsEnabled,
                resultNotificationsEnabled = entity.resultNotificationsEnabled,
                walletNotificationsEnabled = entity.walletNotificationsEnabled,
                securityNotificationsEnabled = true, // mandatory
                operationalNotificationsEnabled = entity.operationalNotificationsEnabled,
                pushEnabled = entity.pushEnabled,
                updatedAt = entity.updatedAt
            )
        } else {
            NotificationPreferenceDto(
                userId = context.accountId,
                gameNotificationsEnabled = true,
                resultNotificationsEnabled = true,
                walletNotificationsEnabled = true,
                securityNotificationsEnabled = true,
                operationalNotificationsEnabled = true,
                pushEnabled = true,
                updatedAt = System.currentTimeMillis()
            )
        }
        return ServerResponse(true, 200, dto, requestId = requestId)
    }

    suspend fun updatePreferences(
        context: SecurityContext,
        request: UpdateNotificationPreferenceRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<NotificationPreferenceDto> {
        val current = preferenceDao.getPreferencesForUser(context.accountId)
        val now = System.currentTimeMillis()

        val updated = NotificationPreferenceEntity(
            userId = context.accountId,
            gameNotificationsEnabled = request.gameNotificationsEnabled ?: current?.gameNotificationsEnabled ?: true,
            resultNotificationsEnabled = request.resultNotificationsEnabled ?: current?.resultNotificationsEnabled ?: true,
            walletNotificationsEnabled = request.walletNotificationsEnabled ?: current?.walletNotificationsEnabled ?: true,
            securityNotificationsEnabled = true, // Security notifications remain mandatory and cannot be disabled
            operationalNotificationsEnabled = request.operationalNotificationsEnabled ?: current?.operationalNotificationsEnabled ?: true,
            pushEnabled = request.pushEnabled ?: current?.pushEnabled ?: true,
            updatedAt = now
        )

        preferenceDao.savePreferences(updated)
        return ServerResponse(
            true,
            200,
            NotificationPreferenceDto(
                userId = updated.userId,
                gameNotificationsEnabled = updated.gameNotificationsEnabled,
                resultNotificationsEnabled = updated.resultNotificationsEnabled,
                walletNotificationsEnabled = updated.walletNotificationsEnabled,
                securityNotificationsEnabled = true,
                operationalNotificationsEnabled = updated.operationalNotificationsEnabled,
                pushEnabled = updated.pushEnabled,
                updatedAt = now
            ),
            null,
            "Notification preferences updated successfully.",
            requestId
        )
    }

    // ==========================================
    // DEVICE TOKEN MANAGEMENT (PUSH INFRASTRUCTURE)
    // ==========================================

    suspend fun registerDevice(
        context: SecurityContext,
        request: RegisterDeviceRequest,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Boolean> {
        val now = System.currentTimeMillis()
        val session = DeviceSessionEntity(
            deviceId = request.deviceId,
            userId = context.accountId,
            pushToken = request.pushToken,
            platform = request.platform,
            appVersion = request.appVersion,
            lastSeenAt = now,
            createdAt = now,
            updatedAt = now,
            revokedAt = null
        )
        deviceSessionDao.registerDevice(session)
        return ServerResponse(true, 200, true, null, "Device session registered.", requestId)
    }

    suspend fun revokeDevice(
        context: SecurityContext,
        deviceId: String,
        requestId: String = UUID.randomUUID().toString()
    ): ServerResponse<Boolean> {
        deviceSessionDao.revokeDevice(deviceId, context.accountId, System.currentTimeMillis())
        return ServerResponse(true, 200, true, null, "Device session revoked.", requestId)
    }

    suspend fun cleanupExpired(now: Long = System.currentTimeMillis()): Int {
        return notificationDao.deleteExpiredNotifications(now)
    }

    private fun NotificationEntity.toDto() = NotificationDto(
        notificationId = notificationId,
        recipientId = recipientId,
        recipientRole = recipientRole,
        type = type,
        title = title,
        body = body,
        severity = severity,
        referenceType = referenceType,
        referenceId = referenceId,
        status = status,
        createdAt = createdAt,
        readAt = readAt,
        expiresAt = expiresAt,
        metadataJson = metadataJson,
        correlationId = correlationId
    )
}
