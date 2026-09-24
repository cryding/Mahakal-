package com.example.backend.model

object NotificationType {
    const val GAME_OPENED = "GAME_OPENED"
    const val GAME_CLOSING = "GAME_CLOSING"
    const val GAME_CLOSED = "GAME_CLOSED"
    const val RESULT_FINALIZED = "RESULT_FINALIZED"
    const val ENTRY_CONFIRMED = "ENTRY_CONFIRMED"
    const val ENTRY_REJECTED = "ENTRY_REJECTED"
    const val ENTRY_REFUNDED = "ENTRY_REFUNDED"
    const val VIRTUAL_COIN_RECEIVED = "VIRTUAL_COIN_RECEIVED"
    const val VIRTUAL_COIN_DEDUCTED = "VIRTUAL_COIN_DEDUCTED"
    const val ACCOUNT_SUSPENDED = "ACCOUNT_SUSPENDED"
    const val ACCOUNT_ACTIVATED = "ACCOUNT_ACTIVATED"
    const val PASSWORD_CHANGED = "PASSWORD_CHANGED"
    const val SECURITY_EVENT = "SECURITY_EVENT"
    const val ADMIN_OPERATIONAL_ALERT = "ADMIN_OPERATIONAL_ALERT"
}

object NotificationSeverity {
    const val INFO = "INFO"
    const val WARNING = "WARNING"
    const val CRITICAL = "CRITICAL"
}

object NotificationStatus {
    const val UNREAD = "UNREAD"
    const val READ = "READ"
    const val ARCHIVED = "ARCHIVED"
}

object NotificationReferenceType {
    const val GAME = "GAME"
    const val ENTRY = "ENTRY"
    const val WALLET_TRANSACTION = "WALLET_TRANSACTION"
    const val ACCOUNT = "ACCOUNT"
    const val SYSTEM = "SYSTEM"
}

data class NotificationDto(
    val notificationId: String,
    val recipientId: String,
    val recipientRole: String,
    val type: String,
    val title: String,
    val body: String,
    val severity: String,
    val referenceType: String?,
    val referenceId: String?,
    val status: String,
    val createdAt: Long,
    val readAt: Long?,
    val expiresAt: Long?,
    val metadataJson: String,
    val correlationId: String?
)

data class NotificationListResponse(
    val notifications: List<NotificationDto>,
    val unreadCount: Int,
    val totalCount: Int,
    val page: Int,
    val pageSize: Int
)

data class UnreadCountResponse(
    val unreadCount: Int,
    val timestamp: Long
)

data class NotificationPreferenceDto(
    val userId: String,
    val gameNotificationsEnabled: Boolean,
    val resultNotificationsEnabled: Boolean,
    val walletNotificationsEnabled: Boolean,
    val securityNotificationsEnabled: Boolean, // always true
    val operationalNotificationsEnabled: Boolean,
    val pushEnabled: Boolean,
    val updatedAt: Long
)

data class UpdateNotificationPreferenceRequest(
    val gameNotificationsEnabled: Boolean? = null,
    val resultNotificationsEnabled: Boolean? = null,
    val walletNotificationsEnabled: Boolean? = null,
    val operationalNotificationsEnabled: Boolean? = null,
    val pushEnabled: Boolean? = null
)

data class RegisterDeviceRequest(
    val deviceId: String,
    val pushToken: String,
    val platform: String = "ANDROID",
    val appVersion: String
)

data class RevokeDeviceRequest(
    val deviceId: String
)
