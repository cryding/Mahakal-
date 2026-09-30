plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.ksp)
}

android {
    namespace = "com.example"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.aistudio.mahakal.ledger"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_BASE_URL", "\"https://dev-api.mahakal.internal/v1\"")
        buildConfigField("String", "API_PORT", "\"8080\"")
        buildConfigField("String", "APP_ENV", "\"development\"")
        buildConfigField("String", "CORS_ALLOWED_ORIGINS", "\"https://admin.mahakal.internal,https://agent.mahakal.internal\"")
        buildConfigField("String", "ACCESS_TOKEN_EXPIRY_SECONDS", "\"3600\"")
        buildConfigField("String", "REFRESH_TOKEN_EXPIRY_SECONDS", "\"604800\"")
        buildConfigField("String", "LOG_LEVEL", "\"INFO\"")
        buildConfigField("String", "PUSH_PROVIDER", "\"FCM\"")
        buildConfigField("String", "PUSH_PROVIDER_PROJECT_ID", "\"mahakal-production\"")
        buildConfigField("String", "DATABASE_CONNECTION_TIMEOUT_MS", "\"5000\"")
        buildConfigField("String", "DATABASE_POOL_MIN_SIZE", "\"10\"")
        buildConfigField("String", "DATABASE_POOL_MAX_SIZE", "\"50\"")
        buildConfigField("String", "DATABASE_SSL_MODE", "\"require\"")
        buildConfigField("String", "OUTBOX_BATCH_SIZE", "\"50\"")
        buildConfigField("String", "OUTBOX_POLL_INTERVAL_MS", "\"10000\"")
        buildConfigField("String", "OUTBOX_MAX_RETRIES", "\"5\"")
        buildConfigField("String", "SENTRY_DSN", "\"PLACEHOLDER_SENTRY_DSN\"")
        buildConfigField("String", "STRUCTURED_JSON_LOGGING", "\"true\"")
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            isMinifyEnabled = false
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)
    implementation(libs.androidx.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)

    implementation(libs.androidx.security.crypto)
    implementation(libs.retrofit)
    implementation(libs.moshi)
    implementation(libs.moshi.kotlin)
    implementation(libs.retrofit.converter.moshi)
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.kotlinx.coroutines.android)

    debugImplementation(libs.androidx.ui.tooling)
}
