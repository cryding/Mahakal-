package com.example.data.repository

import com.example.backend.api.ServerApiRouter
import com.example.backend.model.NotificationDto
import com.example.backend.model.NotificationListResponse
import com.example.backend.model.NotificationPreferenceDto
import com.example.backend.model.RegisterDeviceRequest
import com.example.backend.model.ServerResponse
import com.example.backend.model.UnreadCountResponse
import com.example.backend.model.UpdateNotificationPreferenceRequest
import com.example.backend.service.NotificationService
import com.example.core.security.SecureTokenStorage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filter

interface NotificationRepository {
    suspend fun getNotifications(
        page: Int = 1,
        limit: Int = 20,
        status: String? = null,
        type: String? = null
    ): ServerResponse<NotificationListResponse>

    suspend fun getNotificationDetails(notificationId: String): ServerResponse<NotificationDto>
    suspend fun markAsRead(notificationId: String): ServerResponse<Boolean>
    suspend fun markAllAsRead(): ServerResponse<Int>
    suspend fun archiveNotification(notificationId: String): ServerResponse<Boolean>
    suspend fun getUnreadCount(): ServerResponse<UnreadCountResponse>
    suspend fun getPreferences(): ServerResponse<NotificationPreferenceDto>
    suspend fun updatePreferences(request: UpdateNotificationPreferenceRequest): ServerResponse<NotificationPreferenceDto>
    suspend fun registerDevice(deviceId: String, pushToken: String, appVersion: String): ServerResponse<Boolean>
    suspend fun revokeDevice(deviceId: String): ServerResponse<Boolean>
    fun observeRealtimeNotifications(): Flow<NotificationDto>
}

class NotificationRepositoryImpl(
    private val apiRouter: ServerApiRouter,
    private val secureTokenStorage: SecureTokenStorage,
    private val notificationService: NotificationService
) : NotificationRepository {

    private fun getAuthHeader(): String? {
        val token = secureTokenStorage.getAccessToken() ?: return null
        return "Bearer $token"
    }

    private fun getUserId(): String? {
        return secureTokenStorage.getSessionUserId()
    }

    override suspend fun getNotifications(
        page: Int,
        limit: Int,
        status: String?,
        type: String?
    ): ServerResponse<NotificationListResponse> {
        return apiRouter.handleGetNotifications(
            authHeader = getAuthHeader(),
            page = page,
            limit = limit,
            status = status,
            type = type
        )
    }

    override suspend fun getNotificationDetails(notificationId: String): ServerResponse<NotificationDto> {
        return apiRouter.handleGetNotificationDetails(
            authHeader = getAuthHeader(),
            notificationId = notificationId
        )
    }

    override suspend fun markAsRead(notificationId: String): ServerResponse<Boolean> {
        return apiRouter.handleMarkNotificationRead(
            authHeader = getAuthHeader(),
            notificationId = notificationId
        )
    }

    override suspend fun markAllAsRead(): ServerResponse<Int> {
        return apiRouter.handleMarkAllNotificationsRead(
            authHeader = getAuthHeader()
        )
    }

    override suspend fun archiveNotification(notificationId: String): ServerResponse<Boolean> {
        return apiRouter.handleArchiveNotification(
            authHeader = getAuthHeader(),
            notificationId = notificationId
        )
    }

    override suspend fun getUnreadCount(): ServerResponse<UnreadCountResponse> {
        return apiRouter.handleGetUnreadNotificationCount(
            authHeader = getAuthHeader()
        )
    }

    override suspend fun getPreferences(): ServerResponse<NotificationPreferenceDto> {
        return apiRouter.handleGetNotificationPreferences(
            authHeader = getAuthHeader()
        )
    }

    override suspend fun updatePreferences(request: UpdateNotificationPreferenceRequest): ServerResponse<NotificationPreferenceDto> {
        return apiRouter.handleUpdateNotificationPreferences(
            authHeader = getAuthHeader(),
            request = request
        )
    }

    override suspend fun registerDevice(
        deviceId: String,
        pushToken: String,
        appVersion: String
    ): ServerResponse<Boolean> {
        val request = RegisterDeviceRequest(
            deviceId = deviceId,
            pushToken = pushToken,
            platform = "ANDROID",
            appVersion = appVersion
        )
        return apiRouter.handleRegisterDeviceSession(
            authHeader = getAuthHeader(),
            request = request
        )
    }

    override suspend fun revokeDevice(deviceId: String): ServerResponse<Boolean> {
        return apiRouter.handleRevokeDeviceSession(
            authHeader = getAuthHeader(),
            deviceId = deviceId
        )
    }

    override fun observeRealtimeNotifications(): Flow<NotificationDto> {
        val currentUserId = getUserId()
        return notificationService.realtimeNotificationFlow.filter { notif ->
            currentUserId == null || notif.recipientId == currentUserId
        }
    }
}
