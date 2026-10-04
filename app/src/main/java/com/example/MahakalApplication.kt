package com.example

import android.app.Application
import com.example.core.network.MahakalApiService
import com.example.core.network.MahakalRetrofitClient
import com.example.core.network.SessionManager
import com.example.core.security.SecureTokenStorage
import com.example.data.local.MahakalDatabase
import com.example.data.repository.MahakalRepository

class MahakalApplication : Application() {

    val tokenStorage by lazy { SecureTokenStorage(applicationContext) }
    val sessionManager by lazy { SessionManager(tokenStorage) }
    val apiService: MahakalApiService by lazy { MahakalRetrofitClient.create(tokenStorage) }
    val database: MahakalDatabase by lazy { MahakalDatabase.getInstance(applicationContext) }
    val repository: MahakalRepository by lazy {
        MahakalRepository(
            db = database,
            apiService = apiService,
            sessionManager = sessionManager,
            secureStorage = tokenStorage
        )
    }

    override fun onCreate() {
        super.onCreate()
    }
}
