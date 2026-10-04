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
        versionCode = 2
        versionName = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        buildConfigField("String", "API_PORT", "\"8080\"")
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

    val releaseKeystoreProvider = providers.gradleProperty("mahakal.release.keystore")
        .orElse(providers.gradleProperty("mahakal_release_keystore"))
        .orElse(providers.environmentVariable("MAHAKAL_RELEASE_KEYSTORE"))
        .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_mahakal_release_keystore"))

    val releaseStorePasswordProvider = providers.gradleProperty("mahakal.release.storePassword")
        .orElse(providers.gradleProperty("mahakal_release_storePassword"))
        .orElse(providers.environmentVariable("MAHAKAL_RELEASE_STORE_PASSWORD"))
        .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_mahakal_release_storePassword"))

    val releaseKeyAliasProvider = providers.gradleProperty("mahakal.release.keyAlias")
        .orElse(providers.gradleProperty("mahakal_release_keyAlias"))
        .orElse(providers.environmentVariable("MAHAKAL_RELEASE_KEY_ALIAS"))
        .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_mahakal_release_keyAlias"))

    val releaseKeyPasswordProvider = providers.gradleProperty("mahakal.release.keyPassword")
        .orElse(providers.gradleProperty("mahakal_release_keyPassword"))
        .orElse(providers.environmentVariable("MAHAKAL_RELEASE_KEY_PASSWORD"))
        .orElse(providers.environmentVariable("ORG_GRADLE_PROJECT_mahakal_release_keyPassword"))

    signingConfigs {
        create("release") {
            val ksPath = releaseKeystoreProvider.orNull
            if (!ksPath.isNullOrBlank()) {
                storeFile = file(ksPath)
                storePassword = releaseStorePasswordProvider.orNull
                keyAlias = releaseKeyAliasProvider.orNull ?: "upload"
                keyPassword = releaseKeyPasswordProvider.orNull
                storeType = "PKCS12"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfig = signingConfigs.getByName("release")
            buildConfigField("String", "API_BASE_URL", "\"https://mahakal-qo14.onrender.com\"")
            buildConfigField("String", "APP_ENV", "\"production\"")
        }
        debug {
            isMinifyEnabled = false
            buildConfigField("String", "API_BASE_URL", "\"https://dev-api.mahakal.internal/v1\"")
            buildConfigField("String", "APP_ENV", "\"development\"")
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
    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }
}

tasks.configureEach {
    if (name == "validateSigningRelease" || name == "packageRelease" || name == "signReleaseBundle") {
        doFirst {
            val releaseConfig = android.signingConfigs.getByName("release")
            val ksFile = releaseConfig.storeFile
            if (ksFile == null || !ksFile.exists()) {
                throw GradleException(
                    "Production release build failed: Keystore file is missing or not configured. " +
                    "Expected properties: mahakal.release.keystore, mahakal.release.storePassword, mahakal.release.keyAlias, mahakal.release.keyPassword."
                )
            }
            if (releaseConfig.storePassword.isNullOrBlank() || releaseConfig.keyPassword.isNullOrBlank()) {
                throw GradleException(
                    "Production release build failed: Keystore or Key password is empty. " +
                    "Set mahakal.release.storePassword and mahakal.release.keyPassword."
                )
            }
        }
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
    testImplementation(libs.junit)
    testImplementation(libs.androidx.test.core)
    testImplementation(libs.robolectric)
}
