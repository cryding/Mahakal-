package com.example

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.example.core.network.SessionManager
import com.example.core.security.SecureTokenStorage
import com.example.data.repository.AuthRepositoryImpl
import com.example.ui.auth.LoginScreen
import com.example.ui.auth.LoginViewModel
import com.example.ui.theme.MyApplicationTheme
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = RobolectricDeviceQualifiers.Pixel8, sdk = [36])
class LoginScreenshotTest {

  @get:Rule val composeTestRule = createComposeRule()

  @Test
  fun login_screenshot() {
    val app = ApplicationProvider.getApplicationContext<MahakalApplication>()
    val viewModel = LoginViewModel(app.authRepository)

    composeTestRule.setContent {
      MyApplicationTheme {
        LoginScreen(viewModel = viewModel)
      }
    }

    composeTestRule.onRoot().captureRoboImage(filePath = "src/test/screenshots/login.png")
  }
}

